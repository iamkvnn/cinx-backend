package com.cinx.notification.notification.infrastructure.messaging.event.learning;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** {@code LabReviewDecided} v1 (learning.lab.review.decided): APPROVED or REJECTED, with the admin's reason on a rejection. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabReviewDecidedEvent {
    private String instructorId;
    private String lessonId;
    private String courseId;
    private String lessonTitle;
    private String releaseId;
    private String decision;
    private String reason;
    private Instant occurredAt;
}
