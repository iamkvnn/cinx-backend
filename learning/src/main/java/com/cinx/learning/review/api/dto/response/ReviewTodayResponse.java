package com.cinx.learning.review.api.dto.response;

import com.cinx.learning.review.application.query.ReviewTodayView;
import com.cinx.learning.shared.domain.QuizQuestionType;
import com.cinx.learning.shared.domain.ScoringMethod;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record ReviewTodayResponse(
        @Schema(example = "27", description = "Every question due by today, including those past the daily cap")
        long dueTotal,
        @Schema(example = "20")
        int dailyCap,
        List<Item> items
) {
    public record Item(
            String reviewItemId,
            String courseId,
            String courseTitle,
            String lessonId,
            String lessonTitle,
            @Schema(example = "2", description = "Leitner box 1 to 5")
            int box,
            String questionId,
            String questionText,
            QuizQuestionType questionType,
            ScoringMethod scoringMethod,
            @Schema(description = "As a quiz session shows them: shuffled, with no isCorrect")
            List<Option> options
    ) {
    }

    public record Option(
            @Schema(example = "opt_123")
            String id,
            @Schema(example = "Option text")
            String optionText,
            @Schema(example = "LEFT", description = "A matching question's column")
            String side
    ) {
    }

    public static ReviewTodayResponse of(ReviewTodayView view) {
        return new ReviewTodayResponse(view.dueTotal(), view.dailyCap(), view.items().stream()
                .map(i -> new Item(i.reviewItemId(), i.courseId(), i.courseTitle(), i.lessonId(), i.lessonTitle(), i.box(),
                        i.questionId(), i.questionText(), i.questionType(), i.scoringMethod(),
                        i.options() == null ? List.of() : i.options().stream()
                                .map(o -> new Option(o.id(), o.optionText(), o.side())).toList()))
                .toList());
    }
}
