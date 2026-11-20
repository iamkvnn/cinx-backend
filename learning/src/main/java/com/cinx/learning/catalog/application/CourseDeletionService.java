package com.cinx.learning.catalog.application;

import com.cinx.learning.enrollment.application.EnrollmentAccess;

import com.cinx.learning.catalog.domain.ArticleLessonRepository;
import com.cinx.learning.catalog.domain.AssignmentAttachmentRepository;
import com.cinx.learning.catalog.domain.AssignmentLessonRepository;
import com.cinx.learning.catalog.domain.CourseAccessPolicyRepository;
import com.cinx.learning.catalog.domain.CourseDraftRepository;
import com.cinx.learning.catalog.domain.CourseInstructorMembershipRepository;
import com.cinx.learning.catalog.domain.CourseRepository;
import com.cinx.learning.catalog.domain.CourseReviewNoteRepository;
import com.cinx.learning.catalog.domain.QuizLessonRepository;
import com.cinx.learning.catalog.domain.QuizOptionRepository;
import com.cinx.learning.catalog.domain.QuizQuestionRepository;
import com.cinx.learning.catalog.domain.RejectCourseReasonRepository;
import com.cinx.learning.catalog.domain.SectionRepository;
import com.cinx.learning.catalog.domain.SubtitleJobRepository;
import com.cinx.learning.catalog.domain.SubtitleTrackRepository;
import com.cinx.learning.catalog.domain.VideoLessonRepository;
import com.cinx.learning.catalog.domain.VideoQuestionRepository;

import com.cinx.common.exception.ConflictException;
import com.cinx.common.exception.ErrorCode;
import com.cinx.common.exception.NotFoundException;
import com.cinx.learning.shared.domain.CoursePublishStatus;
import com.cinx.learning.shared.domain.CourseStatus;
import com.cinx.learning.catalog.domain.AssignmentAttachment;
import com.cinx.learning.catalog.domain.Course;
import com.cinx.learning.catalog.domain.CourseImage;
import com.cinx.learning.catalog.domain.Lesson;
import com.cinx.learning.catalog.domain.QuizQuestion;
import com.cinx.learning.catalog.domain.Section;
import com.cinx.learning.catalog.domain.SubtitleTrack;
import com.cinx.learning.shared.application.CourseDeletionHook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseDeletionService implements ICourseDeletionService {
    private final EnrollmentAccess enrollmentAccess;
    private final CourseRepository courseRepository;
    private final CourseDraftRepository courseDraftRepository;
    private final SectionRepository sectionRepository;
    private final VideoLessonRepository videoLessonRepository;
    private final VideoQuestionRepository videoQuestionRepository;
    private final SubtitleTrackRepository subtitleTrackRepository;
    private final SubtitleJobRepository subtitleJobRepository;
    private final ArticleLessonRepository articleLessonRepository;
    private final AssignmentLessonRepository assignmentLessonRepository;
    private final AssignmentAttachmentRepository assignmentAttachmentRepository;
    private final QuizLessonRepository quizLessonRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final QuizOptionRepository quizOptionRepository;
    private final RejectCourseReasonRepository rejectCourseReasonRepository;
    private final CourseReviewNoteRepository courseReviewNoteRepository;
    private final CourseAccessPolicyRepository courseAccessPolicyRepository;
    private final CourseInstructorMembershipRepository membershipRepository;
    private final ICourseAccessService courseAccessService;
    private final ObjectStorage s3Service;
    private final List<CourseDeletionHook> deletionHooks;

    @Override
    @Transactional
    public void deleteDraftCourse(String currentUserId, String courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException("Course not found with id: " + courseId));
        courseAccessService.ensureCanArchiveCourse(currentUserId, course);
        ensureDeletable(course);

        // Lesson content is keyed by stableId, shared by the draft and the published copy.
        List<Section> sections = Stream.concat(
                        sectionRepository.findPublishedByCourse(courseId).stream(),
                        courseDraftRepository.findByCourse(courseId).stream()
                                .flatMap(draft -> sectionRepository.findDraftByDraft(draft.getId()).stream()))
                .toList();
        Set<String> stableIds = new LinkedHashSet<>();
        sections.forEach(section -> section.getLessons().stream().map(Lesson::getStableId).forEach(stableIds::add));

        // features that keep data about the course or its lessons remove it first
        deletionHooks.forEach(hook -> hook.onCourseDeleted(courseId, stableIds));

        List<String> fileKeys = new ArrayList<>();
        stableIds.forEach(stableId -> deleteLessonContent(stableId, fileKeys));
        course.getImages().stream().map(CourseImage::getPublicId).forEach(fileKeys::add);
        fileKeys.add(course.getPromoVideoKey());

        rejectCourseReasonRepository.deleteForCourse(courseId);
        courseReviewNoteRepository.deleteAll(courseReviewNoteRepository.findAllByCourseId(courseId));
        courseAccessPolicyRepository.findById(courseId).ifPresent(courseAccessPolicyRepository::delete);
        membershipRepository.deleteAll(membershipRepository.findByCourseIdOrderByCreatedAtAsc(courseId));

        // Sections cascade to their lessons (and lesson_prerequisite_ids); the course to its images and outcomes.
        sectionRepository.deleteAll(sections);
        courseDraftRepository.findByCourse(courseId).ifPresent(courseDraftRepository::delete);
        courseRepository.delete(course);

        deleteFilesAfterCommit(courseId, fileKeys);
    }

    private void ensureDeletable(Course course) {
        if (course.getStatus() != CourseStatus.DRAFT || course.getPublishedAt() != null) {
            throw new ConflictException(ErrorCode.COURSE_NOT_DELETABLE, "WAS_PUBLISHED");
        }
        if (course.getPublishStatus() == CoursePublishStatus.WAITING_APPROVAL) {
            throw new ConflictException(ErrorCode.COURSE_NOT_DELETABLE, "IN_REVIEW");
        }
        if (enrollmentAccess.courseHasEntitlements(course.getId())) {
            throw new ConflictException(ErrorCode.COURSE_NOT_DELETABLE, "HAS_LEARNERS");
        }
    }

    private void deleteLessonContent(String stableId, List<String> fileKeys) {
        videoLessonRepository.findByLessonId(stableId).ifPresent(video -> {
            fileKeys.add(video.getFileKey());
            List<SubtitleTrack> tracks = subtitleTrackRepository.findByVideoLessonLessonIdOrderByIsDefaultDescLanguageCodeAsc(stableId);
            tracks.forEach(track -> {
                fileKeys.add(track.getFileKey());
                fileKeys.add(track.getOriginalFileKey());
                fileKeys.add(track.getWordConfidenceFileKey());
            });
            subtitleJobRepository.deleteAll(subtitleJobRepository.findByVideoLessonLessonIdOrderByCreatedAtDesc(stableId));
            subtitleTrackRepository.deleteAll(tracks);
            // video questions cascade to their options
            videoQuestionRepository.deleteAll(videoQuestionRepository.findByVideoLessonLessonIdOrderByTimestampSecondsAsc(stableId));
            videoLessonRepository.delete(video);
        });
        articleLessonRepository.findByLessonId(stableId).ifPresent(article -> {
            fileKeys.add(article.getFileKey());
            articleLessonRepository.delete(article);
        });
        assignmentLessonRepository.findByLessonId(stableId).ifPresent(assignment -> {
            List<AssignmentAttachment> attachments = assignment.getAttachments() == null ? List.of() : assignment.getAttachments();
            attachments.stream().map(AssignmentAttachment::getFileKey).forEach(fileKeys::add);
            assignmentAttachmentRepository.deleteAll(attachments);
            assignmentLessonRepository.delete(assignment);
        });
        quizLessonRepository.findByLessonId(stableId).ifPresent(quiz -> {
            List<QuizQuestion> questions = quizQuestionRepository.findAllByQuizLessonId(stableId);
            questions.forEach(question -> quizOptionRepository.deleteAllByQuizQuestionId(question.getId()));
            quizQuestionRepository.deleteAll(questions);
            quizLessonRepository.delete(quiz);
        });
    }

    private void deleteFilesAfterCommit(String courseId, List<String> fileKeys) {
        List<String> keys = fileKeys.stream().filter(k -> k != null && !k.isBlank()).distinct().toList();
        if (keys.isEmpty()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                int failed = s3Service.deleteObjects(keys);
                if (failed > 0) {
                    log.warn("Deleted course {}: {} of {} S3 objects were not deleted", courseId, failed, keys.size());
                }
            }
        });
    }
}
