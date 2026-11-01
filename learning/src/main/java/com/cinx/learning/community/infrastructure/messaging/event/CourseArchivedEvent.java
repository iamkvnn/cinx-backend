package com.cinx.learning.community.infrastructure.messaging.event;

import java.time.LocalDateTime;

public record CourseArchivedEvent(
        CoursePayload course,
        LocalDateTime timestamp
) {
    public record CoursePayload(String id) {
    }
}
