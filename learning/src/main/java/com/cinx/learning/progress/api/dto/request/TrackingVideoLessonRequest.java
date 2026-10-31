package com.cinx.learning.progress.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record TrackingVideoLessonRequest(
        @Schema(example = "120")
        @NotNull(message = "currentPosition must not be null")
        Integer currentPosition
) {
}
