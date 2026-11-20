package com.cinx.learning.community.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record CheckEnrollmentStatus(
        @Schema(example = "course_123") String courseId, 
        @Schema(example = "true") boolean isEnrolled
) {}
