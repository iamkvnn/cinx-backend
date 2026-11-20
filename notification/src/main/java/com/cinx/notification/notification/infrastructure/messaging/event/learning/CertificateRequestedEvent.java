package com.cinx.notification.notification.infrastructure.messaging.event.learning;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CertificateRequestedEvent {
    private String requestId;
    private String userId;
    private String userName;
    private String courseId;
    private String courseTitle;
    private String instructorId;
    private String instructorEmail;
    private Instant occurredAt;
}
