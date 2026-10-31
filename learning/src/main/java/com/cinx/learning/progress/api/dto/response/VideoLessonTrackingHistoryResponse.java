package com.cinx.learning.progress.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record VideoLessonTrackingHistoryResponse(
        @Schema(example = "user_123")
        String userId,
        @Schema(example = "vid_123")
        String videoLessonId,
        @Schema(example = "360")
        Integer currentPosition,
        @Schema(example = "2025-01-01T10:00:00")
        String lastTrackingTime
) {
}
