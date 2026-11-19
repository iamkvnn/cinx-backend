package com.cinx.notification.notification.infrastructure.messaging.event.learning;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseCompletedEvent {
    private String userId;
    private String courseId;
    private String courseTitle;
    private Instant occurredAt;
}
