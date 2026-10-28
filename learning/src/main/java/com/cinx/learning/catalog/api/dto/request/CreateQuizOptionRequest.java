package com.cinx.learning.catalog.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateQuizOptionRequest(
        @NotBlank
        @Schema(example = "Single Responsibility Principle")
        String optionText,
        @NotNull
        @Schema(example = "true")
        Boolean isCorrect,
        @Schema(example = "1")
        Integer optionOrder,
        @Schema(example = "Match Text Example")
        String matchText
) {
}

