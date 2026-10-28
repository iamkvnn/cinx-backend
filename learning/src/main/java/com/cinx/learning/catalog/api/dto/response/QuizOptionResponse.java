package com.cinx.learning.catalog.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record QuizOptionResponse (
        @Schema(example = "opt_123")
        String id,
        @Schema(example = "Single Responsibility Principle")
        String optionText,
        @Schema(example = "true")
        Boolean isCorrect,
        @Schema(example = "1")
        Integer optionOrder,
        @Schema(example = "Match Text Example")
        String matchText
) {}

