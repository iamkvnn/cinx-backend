package com.cinx.notification.notification.infrastructure.messaging.event.learning;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** {@code LabSessionReady} v1 (learning.lab.session.ready): a learner's machine came up after they waited in the queue. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabSessionReadyEvent {
    private String userId;
    private String sessionId;
    private String lessonId;
    private String courseId;
    private String lessonTitle;
    private Instant expiresAt;
    private Instant occurredAt;
}
