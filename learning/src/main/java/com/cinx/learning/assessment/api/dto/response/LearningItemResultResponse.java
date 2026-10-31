package com.cinx.learning.assessment.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record LearningItemResultResponse(
        @Schema(example = "item_123")
        String itemId,
        @Schema(example = "85.5")
        Double score,
        @Schema(example = "10")
        Integer weight
) {
}
