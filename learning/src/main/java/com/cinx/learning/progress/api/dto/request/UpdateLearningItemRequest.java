package com.cinx.learning.progress.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

public record UpdateLearningItemRequest(
        @Schema(example = "true")
        Boolean isCompleted,
        @Schema(example = "true")
        Boolean isPassed,
        @Schema(example = "90.0")
        Double score
) {
}
