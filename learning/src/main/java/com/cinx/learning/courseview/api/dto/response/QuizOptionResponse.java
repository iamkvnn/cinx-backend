package com.cinx.learning.courseview.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record QuizOptionResponse(
        @Schema(example = "opt_123")
        String id,
        @Schema(example = "Polymorphism is the ability of an object to take on many forms.")
        String optionText,
        @Schema(example = "true")
        Boolean isCorrect,
        @Schema(example = "1")
        Integer optionOrder,
        @Schema(example = "Matching text")
        String matchText,
        @Schema(example = "LEFT")
        String side
) {}
