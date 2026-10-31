package com.cinx.learning.assessment.application;

import com.cinx.common.paging.PageQuery;
import com.cinx.common.paging.PageResult;

import com.cinx.common.exception.BadRequestException;
import com.cinx.common.exception.ErrorCode;
import com.cinx.common.exception.NotFoundException;
import com.cinx.learning.shared.domain.DailyGoalType;
import com.cinx.learning.assessment.application.command.CreateAssignmentSubmissionCommand;
import com.cinx.learning.progress.application.command.UpdateLearningItemCommand;
import com.cinx.learning.assessment.application.query.AssignmentSubmissionView;
import com.cinx.learning.assessment.application.mapper.AssignmentSubmissionMapper;
import com.cinx.learning.assessment.domain.AssignmentSubmission;
import com.cinx.learning.assessment.domain.AssignmentSubmissionAttachment;
import com.cinx.learning.assessment.domain.AssignmentSubmissionRepository;
import com.cinx.learning.assessment.domain.AssignmentSubmissionAttachmentRepository;
import com.cinx.learning.engagement.application.IDailyGoalService;
import com.cinx.learning.progress.application.ILearningProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AssignmentService implements IAssignmentService {
    private final AssignmentSubmissionAttachmentRepository assignmentSubmissionAttachmentRepository;
    private final AssignmentSubmissionRepository assignmentSubmissionRepository;
    private final AssignmentSubmissionMapper assignmentSubmissionMapper;
    private final ILearningProgressService learningProgressService;
    private final IDailyGoalService dailyGoalService;
    private final AssessmentAuthorization assessmentAuthorization;
    private final Clock clock;

    @Value("${aws.s3.cdn-url}")
    private String cdnUrl;

    @Override
    public PageResult<AssignmentSubmissionView> getAssignmentSubmissions(String assignmentId, int page, int size, String sort) {
        validatePageRequest(page, size);
        return assignmentSubmissionRepository.findAllByAssignmentId(assignmentId, new PageQuery(page, size, sort))
                .map(assignmentSubmissionMapper::toDto);
    }

    @Override
    public AssignmentSubmissionView getAssignmentSubmission(String userId, String assignmentId) {
        return assignmentSubmissionRepository.findByUserIdAndAssignmentId(userId, assignmentId)
                .map(assignmentSubmissionMapper::toDto)
                .orElseThrow(() -> new NotFoundException("Submission not found"));
    }

    @Transactional
    @Override
    public void submitAssignment(String userId, String assignmentId, CreateAssignmentSubmissionCommand request) {
        if (assignmentSubmissionRepository.findByUserIdAndAssignmentId(userId, assignmentId).isPresent()) {
            throw new BadRequestException(ErrorCode.ASSIGNMENT_ALREADY_SUBMITTED, "You have already submitted this assignment");
        }
        AssignmentSubmission assignmentSubmission = assignmentSubmissionRepository.save(
                AssignmentSubmission.builder()
                        .assignmentId(assignmentId)
                        .userId(userId)
                        .content(request.content())
                        .submissionTime(LocalDateTime.now())
                        .build()
        );

        if (request.attachments() != null && !request.attachments().isEmpty()) {
            assignmentSubmissionAttachmentRepository.saveAll(request.attachments().stream()
                    .<AssignmentSubmissionAttachment>map(file -> AssignmentSubmissionAttachment.builder()       
                            .assignmentSubmissionId(assignmentSubmission.getId())
                            .fileName(file.fileName())
                            .fileType(file.fileType())
                            .fileSize(file.fileSize())
                            .attachmentUrl(cdnUrl + "/" + file.fileKey())
                            .fileKey(file.fileKey())
                            .build())
                    .toList());
        }

        dailyGoalService.recordProgress(userId, DailyGoalType.ASSIGNMENTS_SUBMITTED, 1);
    }

    @Transactional
    @Override
    public void scoreAssignmentSubmission(String currentUserId, String submissionId, Double score) {
        AssignmentSubmission submission = assessmentAuthorization.requireAssignmentSubmissionInstructor(currentUserId, submissionId);
        if (score == null || score < 0.0 || score > 10.0) {
            throw new BadRequestException(ErrorCode.BAD_REQUEST, "Score must be between 0 and 10");
        }
        submission.setScore(score);
        submission.setGradedAt(LocalDateTime.now(clock));
        submission.setGradedBy(currentUserId);
        assignmentSubmissionRepository.save(submission);
        learningProgressService.updateLearningItemProgress(
                submission.getUserId(),
                submission.getAssignmentId(),
                new UpdateLearningItemCommand(true, score >= 5.0, score)
        );
    }

    @Override
    public void deleteAssignmentSubmission(String currentUserId, String submissionId) {
        assessmentAuthorization.requireAssignmentSubmissionDeleteAllowed(currentUserId, submissionId);
        assignmentSubmissionRepository.deleteById(submissionId);
    }

    private void validatePageRequest(int page, int size) {
        if (page < 1) {
            throw new BadRequestException(ErrorCode.INVALID_PAGINATION, "page must be greater than or equal to 1");
        }
        if (size < 1) {
            throw new BadRequestException(ErrorCode.INVALID_PAGINATION, "size must be greater than or equal to 1");
        }
    }
}
