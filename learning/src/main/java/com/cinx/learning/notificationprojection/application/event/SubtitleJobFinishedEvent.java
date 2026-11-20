package com.cinx.learning.notificationprojection.application.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** learning.subtitle.job.finished v1: an AI subtitle job ended, and the instructor who asked for it should review it or retry. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubtitleJobFinishedEvent {
    public static final String REVIEW_REQUIRED = "REVIEW_REQUIRED";
    public static final String FAILED = "FAILED";

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
