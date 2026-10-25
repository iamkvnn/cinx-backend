package com.cinx.learning.catalog.application.event;

import java.time.Instant;

/**
 * The transcripts a published course offers may have changed; a consumer re-reads them all from
 * {@code GET /internal/courses/{courseId}/transcripts}, so the event carries no subtitle content.
 */
public record CourseTranscriptsChanged(String eventId, String courseId, Reason reason, Instant occurredAt) {

    public enum Reason { PUBLISHED, UNARCHIVED, ARCHIVED, TRACK_CHANGED }
}
