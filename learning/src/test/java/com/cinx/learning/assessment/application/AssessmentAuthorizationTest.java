package com.cinx.learning.assessment.application;

import com.cinx.common.exception.ErrorCode;
import com.cinx.common.exception.ForbiddenException;
import com.cinx.common.exception.NotFoundException;
import com.cinx.learning.assessment.domain.AssignmentSubmission;
import com.cinx.learning.assessment.domain.AssignmentSubmissionRepository;
import com.cinx.learning.assessment.domain.QuizSession;
import com.cinx.learning.assessment.domain.QuizSessionRepository;
import com.cinx.learning.shared.application.LearningAuthorizationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssessmentAuthorizationTest {
    @Mock
    private LearningAuthorizationService authorization;
    @Mock
    private QuizSessionRepository quizSessionRepository;
    @Mock
    private AssignmentSubmissionRepository assignmentSubmissionRepository;

    @InjectMocks
    private AssessmentAuthorization assessmentAuthorization;

    @Test
    void theOwnerOfAQuizSessionPasses() {
        QuizSession session = session();
        when(quizSessionRepository.findById("session-1")).thenReturn(Optional.of(session));

        assertThat(assessmentAuthorization.requireQuizSessionOwner("owner-1", "session-1")).isSameAs(session);
    }

    @Test
    void anotherUserIsRefusedEvenAsAnInstructor() {
        when(quizSessionRepository.findById("session-1")).thenReturn(Optional.of(session()));

        assertThatThrownBy(() -> assessmentAuthorization.requireQuizSessionOwner("someone-else", "session-1"))
                .isInstanceOfSatisfying(ForbiddenException.class, e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.NOT_RESOURCE_OWNER));
    }

    @Test
    void theInstructorOfTheLessonMayReadASessionTheyDoNotOwn() {
        QuizSession session = session();
        when(quizSessionRepository.findById("session-1")).thenReturn(Optional.of(session));
        when(authorization.isLessonInstructor("quiz-1", "instructor-1")).thenReturn(true);

        assertThat(assessmentAuthorization.requireQuizSessionOwnerOrInstructor("instructor-1", "session-1")).isSameAs(session);
    }

    @Test
    void aStrangerMayNotReadASession() {
        when(quizSessionRepository.findById("session-1")).thenReturn(Optional.of(session()));
        when(authorization.isLessonInstructor("quiz-1", "stranger")).thenReturn(false);

        assertThatThrownBy(() -> assessmentAuthorization.requireQuizSessionOwnerOrInstructor("stranger", "session-1"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void onlyTheLessonInstructorMayGradeASession() {
        QuizSession session = session();
        when(quizSessionRepository.findById("session-1")).thenReturn(Optional.of(session));
        when(authorization.isLessonInstructor("quiz-1", "owner-1")).thenReturn(false);

        // the learner who took the quiz cannot grade it
        assertThatThrownBy(() -> assessmentAuthorization.requireQuizSessionInstructor("owner-1", "session-1"))
                .isInstanceOfSatisfying(ForbiddenException.class, e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INSTRUCTOR_ACCESS_REQUIRED));
    }

    @Test
    void anUnknownSessionIsNotFoundBeforeAnyAccessDecision() {
        when(quizSessionRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> assessmentAuthorization.requireQuizSessionOwner("owner-1", "missing"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void onlyTheAssignmentInstructorMayGradeASubmission() {
        AssignmentSubmission submission = submission(null);
        when(assignmentSubmissionRepository.findById("sub-1")).thenReturn(Optional.of(submission));
        when(authorization.isLessonInstructor("assignment-1", "learner-1")).thenReturn(false);
        when(authorization.isLessonInstructor("assignment-1", "instructor-1")).thenReturn(true);

        assertThat(assessmentAuthorization.requireAssignmentSubmissionInstructor("instructor-1", "sub-1")).isSameAs(submission);
        assertThatThrownBy(() -> assessmentAuthorization.requireAssignmentSubmissionInstructor("learner-1", "sub-1"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void theOwnerMayDeleteAnUngradedSubmission() {
        AssignmentSubmission submission = submission(null);
        when(assignmentSubmissionRepository.findById("sub-1")).thenReturn(Optional.of(submission));
        when(authorization.isLessonInstructor("assignment-1", "learner-1")).thenReturn(false);

        assertThat(assessmentAuthorization.requireAssignmentSubmissionDeleteAllowed("learner-1", "sub-1")).isSameAs(submission);
    }

    @Test
    void theOwnerMayNotDeleteASubmissionOnceItIsGraded() {
        when(assignmentSubmissionRepository.findById("sub-1")).thenReturn(Optional.of(submission(8.0)));
        when(authorization.isLessonInstructor("assignment-1", "learner-1")).thenReturn(false);

        assertThatThrownBy(() -> assessmentAuthorization.requireAssignmentSubmissionDeleteAllowed("learner-1", "sub-1"))
                .isInstanceOfSatisfying(ForbiddenException.class, e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
    }

    @Test
    void theInstructorMayDeleteAGradedSubmission() {
        AssignmentSubmission submission = submission(8.0);
        when(assignmentSubmissionRepository.findById("sub-1")).thenReturn(Optional.of(submission));
        when(authorization.isLessonInstructor("assignment-1", "instructor-1")).thenReturn(true);

        assertThat(assessmentAuthorization.requireAssignmentSubmissionDeleteAllowed("instructor-1", "sub-1")).isSameAs(submission);
    }

    private QuizSession session() {
        return QuizSession.builder().id("session-1").userId("owner-1").quizLessonId("quiz-1").build();
    }

    private AssignmentSubmission submission(Double score) {
        AssignmentSubmission submission = new AssignmentSubmission();
        submission.setId("sub-1");
        submission.setUserId("learner-1");
        submission.setAssignmentId("assignment-1");
        submission.setScore(score);
        return submission;
    }
}
