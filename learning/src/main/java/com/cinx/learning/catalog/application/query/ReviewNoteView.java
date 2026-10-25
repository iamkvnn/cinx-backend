package com.cinx.learning.catalog.application.query;

import com.cinx.learning.shared.domain.ReviewNoteStatus;
import com.cinx.learning.catalog.domain.ReviewNoteTarget;

import java.time.Instant;
import java.util.List;

public record ReviewNoteView(
        String id,
        String courseId,
        ReviewNoteTarget target,
        String lessonId,
        String lessonTitle,
        String sectionTitle,
        Integer videoSecond,
        String content,
        ReviewNoteStatus status,
        Instant createdAt,
        Instant sentAt
) {
    /** What the instructor sees of the last rejection: the overall reason and the located notes. */
    public record Rejection(String reason, List<ReviewNoteView> notes) {
    }
}
