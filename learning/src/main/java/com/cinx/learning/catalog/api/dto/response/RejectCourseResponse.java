package com.cinx.learning.catalog.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record RejectCourseResponse(
        @Schema(example = "course_123")
        String courseId,
        @Schema(example = "Course content violates copyright policies.")
        String reason,
        @Schema(example = "2025-01-01T10:00:00")
        LocalDateTime rejectedAt
) {
}

