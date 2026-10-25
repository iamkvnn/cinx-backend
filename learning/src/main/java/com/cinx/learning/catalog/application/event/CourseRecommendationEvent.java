package com.cinx.learning.catalog.application.event;

import java.time.LocalDateTime;

public record CourseRecommendationEvent(
        CourseRecommendationPayload course,
        LocalDateTime timestamp
) {
}

