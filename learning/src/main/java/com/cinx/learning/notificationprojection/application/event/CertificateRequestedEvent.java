package com.cinx.learning.notificationprojection.application.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
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
