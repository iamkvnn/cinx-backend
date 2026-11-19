package com.cinx.notification.notification.infrastructure.client.dto.course;

import io.swagger.v3.oas.annotations.media.Schema;

public record CourseResponse(
        @Schema(example = "course_123")
        String id,
        @Schema(example = "Java Bootcamp 2025")
        String title,
        InstructorResponse instructor
) {}