package com.cinx.learning.catalog.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateQuizOptionRequest(
        @Schema(example = "opt_123")
        String id,
        @NotBlank
        @Schema(example = "Updated option text")
        String optionText,
        @NotNull
        @Schema(example = "true")
        Boolean isCorrect,
        @Schema(example = "2")
        Integer optionOrder,
        @Schema(example = "Updated Match Text")
        String matchText
) {}

