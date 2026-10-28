package com.cinx.learning.catalog.infrastructure.messaging.event;

public record LearningEnrollmentGranted(
        String userId,
        String courseId,
        String grantedAt
) {
}
