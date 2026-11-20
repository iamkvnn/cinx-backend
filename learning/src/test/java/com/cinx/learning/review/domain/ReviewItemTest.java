package com.cinx.learning.review.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReviewItemTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 13);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 13, 9, 0);

    private static ReviewItem fresh() {
        return ReviewItem.missedInQuiz("r1", "u1", "c1", "l1", "q1", TODAY, NOW);
    }

    @Test
    void aMissedQuestionStartsInTheFirstBoxTomorrow() {
        ReviewItem item = fresh();
        assertThat(item.getBox()).isEqualTo(1);
        assertThat(item.getDueOn()).isEqualTo(TODAY.plusDays(1));
        assertThat(item.getStatus()).isEqualTo(ReviewItemStatus.ACTIVE);
        assertThat(item.getAlgorithm()).isEqualTo("LEITNER_V1");
        assertThat(item.dueBy(TODAY)).isFalse();
        assertThat(item.dueBy(TODAY.plusDays(1))).isTrue();
    }

    @Test
    void eachRightAnswerMovesUpABoxWithItsInterval() {
        ReviewItem item = fresh();
        int[] expectedDays = {3, 7, 14, 30};
        for (int i = 0; i < expectedDays.length; i++) {
            item = item.answered(true, TODAY, NOW);
            assertThat(item.getBox()).isEqualTo(i + 2);
            assertThat(item.getDueOn()).isEqualTo(TODAY.plusDays(expectedDays[i]));
        }
        assertThat(item.answered(true, TODAY, NOW).getStatus()).isEqualTo(ReviewItemStatus.GRADUATED);
    }

    @Test
    void aWrongAnswerStartsOverTomorrow() {
        ReviewItem item = fresh().answered(true, TODAY, NOW).answered(true, TODAY, NOW);
        ReviewItem wrong = item.answered(false, TODAY, NOW);
        assertThat(wrong.getBox()).isEqualTo(1);
        assertThat(wrong.getDueOn()).isEqualTo(TODAY.plusDays(1));
        assertThat(wrong.getLastReviewedAt()).isEqualTo(NOW);
    }

    @Test
    void missingItInAQuizAgainReopensAGraduatedQuestion() {
        ReviewItem graduated = fresh().toBuilder().box(5).build().answered(true, TODAY, NOW);
        assertThat(graduated.dueBy(TODAY.plusYears(1))).isFalse();

        ReviewItem again = graduated.missedAgain(TODAY.plusDays(40), NOW);
        assertThat(again.getStatus()).isEqualTo(ReviewItemStatus.ACTIVE);
        assertThat(again.getBox()).isEqualTo(1);
        assertThat(again.getDueOn()).isEqualTo(TODAY.plusDays(41));
    }

    @Test
    void aBoxOutsideOneToFiveIsAProgrammingError() {
        assertThatThrownBy(() -> LeitnerSchedule.intervalDays(6)).isInstanceOf(IllegalArgumentException.class);
    }
}
