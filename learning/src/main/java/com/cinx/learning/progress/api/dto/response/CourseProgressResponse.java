package com.cinx.learning.progress.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record CourseProgressResponse(
    @Schema(example = "prog_123")
    String id,
    @Schema(example = "user_123")
    String userId,
    @Schema(example = "course_123")
    String courseId,
    @Schema(example = "true")
    Boolean isCompleted,
    @Schema(example = "true")
    Boolean isPassed,
    @Schema(example = "85.0")
    Double avgScore,
    @Schema(example = "10")
    Integer totalItems,
    @Schema(example = "10")
    Integer completedItems,
    @Schema(example = "2025-01-01T10:00:00")
    LocalDateTime completionTime
) {
}
