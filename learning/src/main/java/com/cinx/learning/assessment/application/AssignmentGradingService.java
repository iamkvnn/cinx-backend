package com.cinx.learning.assessment.application;

import com.cinx.learning.catalog.application.CatalogLookup;

import com.cinx.common.exception.BadRequestException;
import com.cinx.common.exception.ConflictException;
import com.cinx.common.exception.ErrorCode;
import com.cinx.common.exception.ForbiddenException;
import com.cinx.common.exception.NotFoundException;
import com.cinx.learning.catalog.application.query.LearnerSummaryView;
import com.cinx.learning.catalog.application.query.AssignmentLessonInfo;
import com.cinx.learning.catalog.application.ICourseLearnerService;
import com.cinx.learning.catalog.application.ObjectStorage;
import com.cinx.learning.assessment.application.command.GradeSubmissionCommand;
import com.cinx.learning.assessment.application.command.LineCommentCommand;
import com.cinx.learning.assessment.application.command.SaveCriteriaCommand;
import com.cinx.learning.progress.application.command.UpdateLearningItemCommand;
import com.cinx.learning.assessment.application.query.AssignmentGradingView;
import com.cinx.learning.shared.domain.ActivityAction;
import com.cinx.learning.shared.domain.InboxKind;
import com.cinx.learning.instructor.application.IInstructorActivityService;
import com.cinx.learning.instructor.application.InstructorCourseLookup;
import com.cinx.learning.assessment.domain.AssignmentCriterion;
import com.cinx.learning.assessment.domain.AssignmentSubmission;
import com.cinx.learning.assessment.domain.AssignmentSubmissionAttachment;
import com.cinx.learning.assessment.domain.SubmissionCriterionScore;
import com.cinx.learning.assessment.domain.SubmissionGradeAdjustment;
import com.cinx.learning.assessment.domain.SubmissionLineComment;
import com.cinx.learning.assessment.domain.AssignmentCriterionRepository;
import com.cinx.learning.assessment.domain.AssignmentSubmissionRepository;
import com.cinx.learning.assessment.domain.SubmissionCriterionScoreRepository;
import com.cinx.learning.assessment.domain.SubmissionGradeAdjustmentRepository;
import com.cinx.learning.assessment.domain.SubmissionLineCommentRepository;
import com.cinx.learning.shared.application.LearningAuthorizationService;
import com.cinx.learning.progress.application.ILearningProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AssignmentGradingService implements IAssignmentGradingService {
    private static final double EPSILON = 1e-6;
    private static final int LEARNER_BATCH = 100;
    private static final Set<String> TEXT_TYPES = Set.of("application/json", "application/x-yaml", "application/yaml", "application/xml");
    private final CatalogLookup catalogLookup;
    static final long MAX_TEXT_BYTES = 512L * 1024;
    static final double MAX_SCORE = 10.0;
    static final double PASS_SCORE = 5.0;
    private static final Set<String> TEXT_EXTENSIONS = Set.of(".yaml", ".yml", ".json", ".md", ".txt", ".java", ".ts", ".tsx", ".js",
            ".jsx", ".py", ".sql", ".xml", ".properties", ".dockerfile", ".env.example", ".html", ".css", ".sh", ".go", ".kt");

    private final AssignmentSubmissionRepository submissionRepository;
    private final AssignmentCriterionRepository criterionRepository;
    private final SubmissionCriterionScoreRepository scoreRepository;
    private final SubmissionGradeAdjustmentRepository adjustmentRepository;
    private final SubmissionLineCommentRepository commentRepository;
    private final LearningAuthorizationService authorizationService;
    private final AssessmentAuthorization assessmentAuthorization;
    private final ICourseLearnerService courseLearnerService;
    private final ILearningProgressService learningProgressService;
    private final IInstructorActivityService activityService;
    private final InstructorCourseLookup courseLookup;
    private final ObjectStorage s3Service;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public AssignmentGradingView getGrading(String currentUserId, String lessonId) {
        authorizationService.requireLessonInstructor(currentUserId, lessonId);
        InstructorCourseLookup.LessonRef lesson = requireLesson(lessonId);
        String description = catalogLookup.assignmentLesson(lesson.lessonId())
                .map(AssignmentLessonInfo::getDescription)
                .orElse(null);

        List<AssignmentSubmission> submissions = new ArrayList<>(submissionRepository.findAllByAssignmentId(lessonId));
        submissions.sort(Comparator
                .comparing((AssignmentSubmission s) -> s.getScore() != null)
                .thenComparing(s -> s.getScore() == null ? s.getSubmissionTime() : null, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(AssignmentSubmission::getGradedAt, Comparator.nullsLast(Comparator.reverseOrder())));

        List<String> ids = submissions.stream().map(AssignmentSubmission::getId).toList();
        Map<String, List<SubmissionCriterionScore>> scores = ids.isEmpty() ? Map.of()
                : scoreRepository.findAllBySubmissionIdIn(ids).stream().collect(Collectors.groupingBy(SubmissionCriterionScore::getSubmissionId));
        Map<String, List<SubmissionGradeAdjustment>> adjustments = ids.isEmpty() ? Map.of()
                : adjustmentRepository.findAllBySubmissionIdIn(ids).stream().collect(Collectors.groupingBy(SubmissionGradeAdjustment::getSubmissionId));
        Map<String, List<SubmissionLineComment>> comments = ids.isEmpty() ? Map.of()
                : commentRepository.findAllBySubmissionIdInOrderByLineAsc(ids).stream().collect(Collectors.groupingBy(SubmissionLineComment::getSubmissionId));
        Map<String, LearnerSummaryView> learners = learners(currentUserId, lesson.courseId(),
                submissions.stream().map(AssignmentSubmission::getUserId).filter(Objects::nonNull).distinct().toList());

        return new AssignmentGradingView(
                lessonId,
                lesson.courseId(),
                lesson.title(),
                description,
                criterionRepository.findAllByAssignmentIdOrderByOrderIndexAsc(lessonId).stream().map(AssignmentGradingService::toCriterion).toList(),
                submissions.stream()
                        .map(s -> toSubmission(s, learners.get(s.getUserId()),
                                scores.getOrDefault(s.getId(), List.of()),
                                adjustments.getOrDefault(s.getId(), List.of()),
                                comments.getOrDefault(s.getId(), List.of())))
                        .toList());
    }

    @Override
    @Transactional
    public List<AssignmentGradingView.Criterion> saveCriteria(String currentUserId, String lessonId, SaveCriteriaCommand request) {
        authorizationService.requireLessonInstructor(currentUserId, lessonId);
        List<SaveCriteriaCommand.CriterionInput> inputs = request.criteria();
        double total = 0;
        for (SaveCriteriaCommand.CriterionInput input : inputs) {
            requireHalfStep(input.maxPoints(), "maxPoints");
            total += input.maxPoints();
        }
        if (!inputs.isEmpty() && Math.abs(total - MAX_SCORE) > EPSILON) {
            throw new BadRequestException(ErrorCode.CRITERIA_TOTAL_NOT_10, "Criteria must add up to 10 points, got " + total);
        }

        List<AssignmentCriterion> existing = criterionRepository.findAllByAssignmentIdOrderByOrderIndexAsc(lessonId);
        Map<String, AssignmentCriterion> byId = existing.stream().collect(Collectors.toMap(AssignmentCriterion::getId, c -> c));
        List<AssignmentCriterion> kept = new ArrayList<>();
        for (int i = 0; i < inputs.size(); i++) {
            SaveCriteriaCommand.CriterionInput input = inputs.get(i);
            AssignmentCriterion criterion = input.id() != null && byId.containsKey(input.id())
                    ? byId.remove(input.id())
                    : AssignmentCriterion.builder().assignmentId(lessonId).build();
            criterion.setTitle(input.title().trim());
            criterion.setDescription(blankToNull(input.description()));
            criterion.setMaxPoints(input.maxPoints());
            criterion.setOrderIndex(i);
            kept.add(criterion);
        }
        if (!byId.isEmpty()) {
            scoreRepository.deleteAllByCriterionIdIn(byId.keySet());
            criterionRepository.deleteAll(byId.values());
        }
        return criterionRepository.saveAll(kept).stream().map(AssignmentGradingService::toCriterion).toList();
    }

    @Override
    @Transactional
    public AssignmentGradingView.Submission grade(String currentUserId, String submissionId, GradeSubmissionCommand request) {
        AssignmentSubmission submission = assessmentAuthorization.requireAssignmentSubmissionInstructor(currentUserId, submissionId);
        if (!Objects.equals(submission.getVersion(), request.expectedVersion())) {
            throw new ConflictException(ErrorCode.SUBMISSION_ALREADY_GRADED, "The submission changed since it was opened");
        }
        double total = request.total();
        if (total < 0 || total > MAX_SCORE) {
            throw new BadRequestException(ErrorCode.GRADE_TOTAL_OUT_OF_RANGE, "Total must be between 0 and 10");
        }
        requireHalfStep(total, "total");

        List<AssignmentCriterion> criteria = criterionRepository.findAllByAssignmentIdOrderByOrderIndexAsc(submission.getAssignmentId());
        List<SubmissionCriterionScore> scores = criterionScores(submission.getId(), criteria, request.criterionScores());
        List<SubmissionGradeAdjustment> adjustments = request.adjustments().stream()
                .<SubmissionGradeAdjustment>map(a -> {
                    requireHalfStep(a.points(), "adjustment points");
                    return SubmissionGradeAdjustment.builder()
                            .submissionId(submission.getId())
                            .points(a.points())
                            .reason(a.reason().trim())
                            .build();
                })
                .toList();

        scoreRepository.deleteAllBySubmissionId(submission.getId());
        adjustmentRepository.deleteAllBySubmissionId(submission.getId());
        List<SubmissionCriterionScore> savedScores = scoreRepository.saveAll(scores);
        List<SubmissionGradeAdjustment> savedAdjustments = adjustmentRepository.saveAll(adjustments);

        submission.setScore(total);
        submission.setFeedback(blankToNull(request.feedback()));
        submission.setGradedAt(LocalDateTime.now(clock));
        submission.setGradedBy(currentUserId);
        submission.setAiAssisted(Boolean.TRUE.equals(request.aiAssisted()));
        AssignmentSubmission saved;
        try {
            saved = submissionRepository.saveAndFlush(submission);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new ConflictException(ErrorCode.SUBMISSION_ALREADY_GRADED, "The submission was graded by someone else");
        }

        learningProgressService.updateLearningItemProgress(
                saved.getUserId(),
                saved.getAssignmentId(),
                new UpdateLearningItemCommand(true, total >= PASS_SCORE, total));

        InstructorCourseLookup.LessonRef lesson = courseLookup.lessonsByStableId(List.of(saved.getAssignmentId())).get(saved.getAssignmentId());
        if (lesson != null) {
            activityService.record(currentUserId, lesson.courseId(), InboxKind.GRADING, ActivityAction.GRADED,
                    saved.getAssignmentId(), 1, lesson.title() + " · " + total);
        }

        Map<String, LearnerSummaryView> learner = lesson == null ? Map.of()
                : learners(currentUserId, lesson.courseId(), List.of(saved.getUserId()));
        return toSubmission(saved, learner.get(saved.getUserId()), savedScores, savedAdjustments,
                commentRepository.findAllBySubmissionIdInOrderByLineAsc(List.of(saved.getId())));
    }

    @Override
    @Transactional
    public AssignmentGradingView.LineComment addComment(String currentUserId, String submissionId, LineCommentCommand request) {
        AssignmentSubmission submission = assessmentAuthorization.requireAssignmentSubmissionInstructor(currentUserId, submissionId);
        requireAttachment(submission, request.attachmentId());
        SubmissionLineComment comment = commentRepository.save(SubmissionLineComment.builder()
                .submissionId(submission.getId())
                .attachmentId(request.attachmentId())
                .line(request.line())
                .content(request.content().trim())
                .authorId(currentUserId)
                .build());
        return toComment(comment);
    }

    @Override
    @Transactional
    public void deleteComment(String currentUserId, String submissionId, String commentId) {
        AssignmentSubmission submission = assessmentAuthorization.requireAssignmentSubmissionInstructor(currentUserId, submissionId);
        SubmissionLineComment comment = commentRepository.findById(commentId)
                .filter(c -> c.getSubmissionId().equals(submission.getId()))
                .orElseThrow(() -> new NotFoundException("Comment not found"));
        if (!comment.getAuthorId().equals(currentUserId)) {
            throw new ForbiddenException(ErrorCode.NOT_RESOURCE_OWNER, "Only the author can delete this comment");
        }
        commentRepository.delete(comment);
    }

    @Override
    @Transactional(readOnly = true)
    public AssignmentGradingView.AttachmentText readAttachmentText(String currentUserId, String submissionId, String attachmentId) {
        AssignmentSubmission submission = assessmentAuthorization.requireAssignmentSubmissionInstructor(currentUserId, submissionId);
        AssignmentSubmissionAttachment attachment = requireAttachment(submission, attachmentId);
        if (!isTextType(attachment.getFileName(), attachment.getFileType())) {
            throw new BadRequestException(ErrorCode.ATTACHMENT_NOT_TEXT, "This attachment is not a text file");
        }
        if (attachment.getFileSize() == null || attachment.getFileSize() > MAX_TEXT_BYTES) {
            throw new BadRequestException(ErrorCode.ATTACHMENT_TOO_LARGE, "Only files up to 512 KB are shown inline");
        }
        return new AssignmentGradingView.AttachmentText(s3Service.readTextObject(attachment.getFileKey()));
    }

    /** Text the viewer can show inline: by content type or by file extension, and small enough. */
    static boolean isTextReadable(AssignmentSubmissionAttachment attachment) {
        return isTextType(attachment.getFileName(), attachment.getFileType())
                && attachment.getFileSize() != null
                && attachment.getFileSize() <= MAX_TEXT_BYTES;
    }

    static boolean isTextType(String fileName, String fileType) {
        String type = fileType == null ? "" : fileType.toLowerCase(Locale.ROOT);
        if (type.startsWith("text/") || TEXT_TYPES.contains(type)) {
            return true;
        }
        String name = fileName == null ? "" : fileName.toLowerCase(Locale.ROOT);
        return name.equals("dockerfile") || TEXT_EXTENSIONS.stream().anyMatch(name::endsWith);
    }

    private List<SubmissionCriterionScore> criterionScores(String submissionId, List<AssignmentCriterion> criteria,
                                                           List<GradeSubmissionCommand.CriterionScoreInput> inputs) {
        if (criteria.isEmpty()) {
            if (!inputs.isEmpty()) {
                throw new BadRequestException(ErrorCode.BAD_REQUEST, "This assignment has no criteria");
            }
            return List.of();
        }
        Map<String, AssignmentCriterion> byId = criteria.stream().collect(Collectors.toMap(AssignmentCriterion::getId, c -> c));
        Set<String> seen = new HashSet<>();
        List<SubmissionCriterionScore> scores = new ArrayList<>();
        for (GradeSubmissionCommand.CriterionScoreInput input : inputs) {
            AssignmentCriterion criterion = byId.get(input.criterionId());
            if (criterion == null || !seen.add(input.criterionId())) {
                throw new BadRequestException(ErrorCode.BAD_REQUEST, "Unknown or repeated criterion " + input.criterionId());
            }
            requireHalfStep(input.points(), "points");
            if (input.points() > criterion.getMaxPoints() + EPSILON) {
                throw new BadRequestException(ErrorCode.BAD_REQUEST, "Points above the criterion maximum");
            }
            scores.add(SubmissionCriterionScore.builder()
                    .submissionId(submissionId)
                    .criterionId(criterion.getId())
                    .points(input.points())
                    .build());
        }
        if (seen.size() != criteria.size()) {
            throw new BadRequestException(ErrorCode.BAD_REQUEST, "Every criterion needs a score");
        }
        return scores;
    }

    private Map<String, LearnerSummaryView> learners(String currentUserId, String courseId, List<String> userIds) {
        Map<String, LearnerSummaryView> result = new HashMap<>();
        for (int from = 0; from < userIds.size(); from += LEARNER_BATCH) {
            List<String> chunk = userIds.subList(from, Math.min(userIds.size(), from + LEARNER_BATCH));
            courseLearnerService.summaries(currentUserId, courseId, chunk).forEach(l -> result.put(l.userId(), l));
        }
        return result;
    }

    private InstructorCourseLookup.LessonRef requireLesson(String lessonId) {
        InstructorCourseLookup.LessonRef lesson = courseLookup.lessonsByStableId(List.of(lessonId)).get(lessonId);
        if (lesson == null) {
            throw new NotFoundException("Assignment not found");
        }
        return lesson;
    }

    private static AssignmentSubmissionAttachment requireAttachment(AssignmentSubmission submission, String attachmentId) {
        List<AssignmentSubmissionAttachment> attachments = submission.getAttachments() == null ? List.of() : submission.getAttachments();
        return attachments.stream()
                .filter(a -> a.getId().equals(attachmentId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Attachment not found"));
    }

    private static void requireHalfStep(Double value, String field) {
        if (value == null || Math.abs(value * 2 - Math.rint(value * 2)) > EPSILON) {
            throw new BadRequestException(ErrorCode.BAD_REQUEST, field + " must be a multiple of 0.5");
        }
    }

    private static AssignmentGradingView.Criterion toCriterion(AssignmentCriterion c) {
        return new AssignmentGradingView.Criterion(c.getId(), c.getTitle(), c.getDescription(), c.getMaxPoints(), c.getOrderIndex());
    }

    private static AssignmentGradingView.LineComment toComment(SubmissionLineComment c) {
        return new AssignmentGradingView.LineComment(c.getId(), c.getAttachmentId(), c.getLine(), c.getContent(), c.getAuthorId(),
                c.getCreatedAt() == null ? null : c.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant());
    }

    private static AssignmentGradingView.Submission toSubmission(AssignmentSubmission s, LearnerSummaryView learner,
                                                                    List<SubmissionCriterionScore> scores,
                                                                    List<SubmissionGradeAdjustment> adjustments,
                                                                    List<SubmissionLineComment> comments) {
        List<AssignmentSubmissionAttachment> attachments = s.getAttachments() == null ? List.of() : s.getAttachments();
        return new AssignmentGradingView.Submission(
                s.getId(),
                s.getUserId(),
                learner == null ? null : learner.name(),
                learner == null ? null : learner.avatarUrl(),
                s.getContent(),
                s.getSubmissionTime() == null ? null : s.getSubmissionTime().atZone(ZoneId.systemDefault()).toInstant(),
                1,
                s.getVersion(),
                attachments.stream()
                        .map(a -> new AssignmentGradingView.Attachment(a.getId(), a.getFileName(), a.getFileType(), a.getFileSize(),
                                a.getAttachmentUrl(), isTextReadable(a)))
                        .toList(),
                s.getScore(),
                s.getFeedback(),
                s.getGradedAt() == null ? null : s.getGradedAt().toInstant(ZoneOffset.UTC),
                s.getGradedBy(),
                Boolean.TRUE.equals(s.getAiAssisted()),
                scores.stream().map(x -> new AssignmentGradingView.CriterionScore(x.getCriterionId(), x.getPoints())).toList(),
                adjustments.stream().map(x -> new AssignmentGradingView.Adjustment(x.getId(), x.getPoints(), x.getReason())).toList(),
                comments.stream().map(AssignmentGradingService::toComment).toList());
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
