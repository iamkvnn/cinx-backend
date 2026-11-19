package com.cinx.notification.notification.infrastructure.messaging.event.learning;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** {@code LabSessionEnding} v1 (learning.lab.session.ending): {@code minutesLeft} is 10, then 2. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabSessionEndingEvent {
    private String userId;
    private String sessionId;
    private String lessonId;
    private String courseId;
    private String lessonTitle;
    private int minutesLeft;
    private Instant expiresAt;
    private Instant occurredAt;
}
