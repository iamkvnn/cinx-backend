package com.cinx.notification.notification.infrastructure.messaging.event.learning;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** learning.subtitle.job.finished v1: an AI subtitle job ended; its requester reviews the track or retries. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubtitleJobFinishedEvent {
    private String jobId;
    private String userId;
    private String courseId;
    private String lessonId;
    private String lessonTitle;
    private String languageCode;
    private String displayName;
    /** REVIEW_REQUIRED or FAILED */
    private String outcome;
    private int warningCount;
    private String actionUrl;
    private Instant occurredAt;
}
