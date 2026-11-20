package com.cinx.learning.catalog.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record VideoOptionResponse(
        @Schema(example = "opt_123")
        String id,
        @Schema(example = "Auto-configuration")
        String optionText,
        @Schema(example = "true")
        Boolean isCorrect
) {
}
