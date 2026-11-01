package com.cinx.learning.review.api.dto.response;

import com.cinx.learning.review.application.query.ReviewAnswerView;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

public record ReviewAnswerResponse(
        boolean correct,
        @Schema(example = "0.5", description = "0 to 1, as a quiz session scores the question; only 1 counts as correct")
        double score,
        @Schema(description = "The key in the quiz session snapshot's format; null when the question can no longer be read")
        String correctAnswer,
        String explanation,
        @Schema(example = "3", description = "The Leitner box after this answer")
        int box,
        @Schema(example = "2026-10-20", description = "The next review day in Vietnam time; null once graduated")
        LocalDate nextDueOn,
        boolean graduated
) {
    public static ReviewAnswerResponse of(ReviewAnswerView view) {
        return new ReviewAnswerResponse(view.correct(), view.score(), view.correctAnswer(), view.explanation(), view.box(),
                view.nextDueOn(), view.graduated());
    }
}
