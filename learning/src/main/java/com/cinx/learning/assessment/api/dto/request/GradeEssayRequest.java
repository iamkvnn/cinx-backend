package com.cinx.learning.assessment.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record GradeEssayRequest(
        @NotNull
        @NotEmpty
        @Valid
        List<EssayQuestionScore> scores
) {
    public record EssayQuestionScore(
            @Schema(example = "q_123")
            @NotNull String questionId,
            @Schema(example = "10.0")
            @NotNull
            @DecimalMin(value = "0.0", message = "Essay score must be at least 0")
            @DecimalMax(value = "10.0", message = "Essay score must be at most 10")
            Double score
    ) {}
}
