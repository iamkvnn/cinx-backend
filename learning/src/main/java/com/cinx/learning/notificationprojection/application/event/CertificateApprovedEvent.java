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
public class CertificateApprovedEvent {
    private String requestId;
    private String userId;
    private String userName;
    private String userEmail;
    private String courseId;
    private String courseTitle;
    private String certificateUrl;
    private Instant occurredAt;
}
