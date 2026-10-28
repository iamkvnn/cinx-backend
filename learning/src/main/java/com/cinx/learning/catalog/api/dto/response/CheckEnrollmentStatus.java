package com.cinx.learning.catalog.api.dto.response;

public record CheckEnrollmentStatus(
        String courseId,
        boolean isEnrolled
) {
}

