package com.cinx.learning.progress.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record UpdateCourseProgressRequest(
    @Schema(example = "true")
    @NotNull(message = "isCompleted must not be null")
    Boolean isCompleted,

    @Schema(example = "true")
    @NotNull(message = "isPassed must not be null")
    Boolean isPassed,

    @Schema(example = "85.0")
    @NotNull(message = "avgScore must not be null")
    Double avgScore,

    @Schema(example = "10")
    @NotNull(message = "totalItems must not be null")
    Integer totalItems,

    @Schema(example = "8")
    @NotNull(message = "completedItems must not be null")
    Integer completedItems,

    @Schema(example = "2025-01-01T10:00:00")
    @NotNull(message = "completionTime must not be null")
    LocalDateTime completionTime
) {
}
