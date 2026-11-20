package com.cinx.learning.catalog.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateVideoOptionRequest(
        @NotBlank
        @Schema(example = "Auto-configuration")
        String optionText,
        @NotNull
        @Schema(example = "true")
        Boolean isCorrect
) {
}

