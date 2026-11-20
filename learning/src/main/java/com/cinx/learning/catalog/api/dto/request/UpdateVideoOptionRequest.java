package com.cinx.learning.catalog.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record UpdateVideoOptionRequest(
        @Schema(example = "opt_123")
        String id,
        @NotBlank
        @Schema(example = "Updated option text")
        String optionText,
        @Schema(example = "false")
        Boolean isCorrect
) {
}
