package com.cinx.learning.assessment.application;

import com.cinx.common.exception.ErrorCode;
import com.cinx.common.exception.ForbiddenException;
import com.cinx.common.exception.NotFoundException;
import com.cinx.learning.assessment.domain.AssignmentSubmission;
import com.cinx.learning.assessment.domain.AssignmentSubmissionRepository;
import com.cinx.learning.assessment.domain.QuizSession;
import com.cinx.learning.assessment.domain.QuizSessionRepository;
import com.cinx.learning.shared.application.LearningAuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Objects;

/** Who may see or change a quiz session or an assignment submission: its owner or the instructor of its lesson. */
@Service
@RequiredArgsConstructor
public class AssessmentAuthorization {
    private final LearningAuthorizationService authorization;
    private final QuizSessionRepository quizSessionRepository;
    private final AssignmentSubmissionRepository assignmentSubmissionRepository;

    public QuizSession requireQuizSessionOwner(String currentUserId, String sessionId) {
        QuizSession session = findQuizSession(sessionId);
        if (Objects.equals(session.getUserId(), currentUserId)) {
            return session;
        }
        throw new ForbiddenException(ErrorCode.NOT_RESOURCE_OWNER, "Only the quiz session owner can access this resource");
    }

    public QuizSession requireQuizSessionOwnerOrInstructor(String currentUserId, String sessionId) {
        QuizSession session = findQuizSession(sessionId);
        if (Objects.equals(session.getUserId(), currentUserId) || authorization.isLessonInstructor(session.getQuizLessonId(), currentUserId)) {
            return session;
        }
        throw new ForbiddenException(ErrorCode.FORBIDDEN, "Only the quiz session owner or lesson instructor can access this resource");
    }

    public QuizSession requireQuizSessionInstructor(String currentUserId, String sessionId) {
        QuizSession session = findQuizSession(sessionId);
        if (authorization.isLessonInstructor(session.getQuizLessonId(), currentUserId)) {
            return session;
        }
        throw new ForbiddenException(ErrorCode.INSTRUCTOR_ACCESS_REQUIRED, "Only the lesson instructor can access this resource");
    }

    public AssignmentSubmission requireAssignmentSubmissionInstructor(String currentUserId, String submissionId) {
        AssignmentSubmission submission = findAssignmentSubmission(submissionId);
        if (authorization.isLessonInstructor(submission.getAssignmentId(), currentUserId)) {
            return submission;
        }
        throw new ForbiddenException(ErrorCode.INSTRUCTOR_ACCESS_REQUIRED, "Only the assignment instructor can access this resource");
    }

    public AssignmentSubmission requireAssignmentSubmissionDeleteAllowed(String currentUserId, String submissionId) {
        AssignmentSubmission submission = findAssignmentSubmission(submissionId);
        if (authorization.isLessonInstructor(submission.getAssignmentId(), currentUserId)) {
            return submission;
        }
        if (Objects.equals(submission.getUserId(), currentUserId) && submission.getScore() == null) {
            return submission;
        }
        throw new ForbiddenException(ErrorCode.FORBIDDEN, "Only the submission owner before grading or assignment instructor can delete this submission");
    }

    private QuizSession findQuizSession(String sessionId) {
        return quizSessionRepository.findById(sessionId)
                .orElseThrow(() -> new NotFoundException("Quiz session not found"));
    }

    private AssignmentSubmission findAssignmentSubmission(String submissionId) {
        return assignmentSubmissionRepository.findById(submissionId)
                .orElseThrow(() -> new NotFoundException("Submission not found"));
    }
}
