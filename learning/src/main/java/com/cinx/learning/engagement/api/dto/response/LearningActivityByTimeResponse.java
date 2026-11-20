package com.cinx.learning.engagement.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record LearningActivityByTimeResponse(
        @Schema(example = "2026-05")
        String timeLabel,
        @Schema(example = "100800")
        Long activeSeconds
) {
}
