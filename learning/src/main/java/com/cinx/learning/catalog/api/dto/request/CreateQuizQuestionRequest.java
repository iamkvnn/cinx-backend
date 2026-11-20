package com.cinx.learning.catalog.api.dto.request;

import com.cinx.learning.catalog.domain.QuizQuestionType;
import com.cinx.learning.catalog.domain.ScoringMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateQuizQuestionRequest(
        @NotBlank
        @Size(max = 2000)
        @Schema(example = "What does SOLID stand for in Java?", maxLength = 2000)
        String questionText,
        @NotNull
        @Schema(example = "SINGLE_CHOICE")
        QuizQuestionType questionType,
        @NotNull
        @Schema(example = "EXACT")
        ScoringMethod scoringMethod,
        @Valid
        @Schema(description = "Required for every type except ESSAY, which has none")
        List<CreateQuizOptionRequest> options,
        @Size(max = 4000)
        @Schema(description = "ESSAY only; ignored for other types")
        String referenceAnswer,
        @Size(max = 2000)
        @Schema(description = "Why the answer is right; learners see it on review when the quiz shows answers")
        String explanation,
        @Schema(description = "true when an AI draft helped write the question; learners see a label")
        Boolean aiAssisted
) {
}

