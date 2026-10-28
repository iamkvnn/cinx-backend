package com.cinx.learning.catalog.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SyncQuizRequest(
        @NotNull 
        @Schema(example = "true")
        Boolean triggerRegrade,
        @NotBlank 
        @Schema(example = "Fixed typo in question 2")
        String changeReason
) {}

