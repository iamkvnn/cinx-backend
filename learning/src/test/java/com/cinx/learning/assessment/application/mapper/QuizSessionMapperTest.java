package com.cinx.learning.assessment.application.mapper;

import com.cinx.learning.assessment.application.query.QuizSessionView;
import com.cinx.learning.assessment.domain.QuizSession;
import com.cinx.learning.assessment.domain.QuizSessionSubmission;
import com.cinx.learning.shared.domain.QuizSessionStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class QuizSessionMapperTest {
    private final QuizSessionMapperImpl mapper = new QuizSessionMapperImpl();

    @Test
    void theSessionAndItsSubmissionKeepTheirOwnIdsAndUserIds() {
        QuizSession session = QuizSession.builder().id("session-1").userId("learner-1").quizLessonId("quiz-1")
                .status(QuizSessionStatus.GRADED).isReviewAllowed(true).isShowAnswersOnReview(false)
                .startTime(LocalDateTime.of(2026, 10, 7, 1, 0)).endTime(LocalDateTime.of(2026, 10, 7, 1, 30)).build();
        QuizSessionSubmission submission = QuizSessionSubmission.builder().id("sub-9").userId("other-user").quizSessionId("session-1")
                .score(7.5).totalCorrectAnswers(3).build();

        QuizSessionView view = mapper.toDto(session, submission);

        assertThat(view.id()).isEqualTo("session-1");
        assertThat(view.userId()).isEqualTo("learner-1");
        assertThat(view.status()).isEqualTo(QuizSessionStatus.GRADED);
        assertThat(view.isShowAnswersOnReview()).isFalse();
        assertThat(view.quizSessionSubmission().id()).isEqualTo("sub-9");
        assertThat(view.quizSessionSubmission().score()).isEqualTo(7.5);
        assertThat(view.quizSessionSubmission().totalCorrectAnswers()).isEqualTo(3);
    }

    @Test
    void aSessionWithoutASubmissionHasNone() {
        QuizSession session = QuizSession.builder().id("session-1").userId("learner-1").status(QuizSessionStatus.IN_PROGRESS).build();

        QuizSessionView view = mapper.toDto(session, null);

        assertThat(view.id()).isEqualTo("session-1");
        assertThat(view.quizSessionSubmission()).isNull();
    }
}
