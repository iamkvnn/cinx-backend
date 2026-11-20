package com.cinx.learning.catalog.api.dto.response;

import com.cinx.learning.catalog.domain.QuizQuestionType;
import com.cinx.learning.catalog.domain.ScoringMethod;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record QuizQuestionResponse(
        @Schema(example = "qq_123")
        String id,
        @Schema(example = "What does SOLID stand for in Java?")
        String questionText,
        @Schema(example = "SINGLE_CHOICE")
        QuizQuestionType questionType,
        @Schema(example = "EXACT")
        ScoringMethod scoringMethod,
        @Schema(example = "false")
        Boolean needSync,
        List<QuizOptionResponse> options,
        @Schema(description = "Essay questions only. Course members see it; learners never do.")
        String referenceAnswer,
        @Schema(description = "Why the answer is right. Course members see it; learners only on review, through the quiz session.")
        String explanation,
        @Schema(description = "An AI draft helped write the question", example = "false")
        Boolean aiAssisted
) {
    public QuizQuestionResponse withoutReferenceAnswer() {
        return referenceAnswer == null ? this
                : new QuizQuestionResponse(id, questionText, questionType, scoringMethod, needSync, options, null, explanation,
                        aiAssisted);
    }

    /**
     * The view a learner gets before an attempt: no isCorrect, no reference answer, and none of the fields that
     * encode the key by type (a short-text option is the accepted answer, a matchText is the pair, an ordering's
     * optionOrder is the sequence).
     */
    public QuizQuestionResponse withoutAnswerKeys() {
        List<QuizOptionResponse> hidden = options == null ? null
                : questionType == QuizQuestionType.SHORT_TEXT ? List.of()
                : options.stream()
                        .map(o -> new QuizOptionResponse(o.id(), o.optionText(), null,
                                questionType == QuizQuestionType.ORDERING ? null : o.optionOrder(),
                                questionType == QuizQuestionType.MATCHING ? null : o.matchText()))
                        .toList();
        return new QuizQuestionResponse(id, questionText, questionType, scoringMethod, needSync, hidden, null, null, aiAssisted);
    }
}

