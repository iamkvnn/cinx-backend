package com.cinx.learning.progress.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record LearningItemProgressResponse(
        @Schema(example = "item_123")
        String itemId,
        @Schema(example = "true")
        Boolean isCompleted,
        @Schema(example = "true")
        Boolean isPassed,
        @Schema(example = "90.0")
        Double score
) {
}
