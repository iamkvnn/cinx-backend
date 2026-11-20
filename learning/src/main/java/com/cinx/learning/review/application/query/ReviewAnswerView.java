package com.cinx.learning.review.application.query;

import java.time.LocalDate;

/**
 * The result of one review answer.
 *
 * @param correctAnswer the key in the quiz session snapshot's format; null when the question is no longer readable
 * @param nextDueOn     the next review day (Asia/Ho_Chi_Minh); null once graduated
 */
public record ReviewAnswerView(
        boolean correct,
        double score,
        String correctAnswer,
        String explanation,
        int box,
        LocalDate nextDueOn,
        boolean graduated) {
}
