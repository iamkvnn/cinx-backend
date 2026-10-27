package com.cinx.learning.community.application.query;

import com.cinx.learning.community.domain.ReportType;

import java.time.LocalDateTime;

public record ReportedContentView(
        String id,
        ReportType type,
        String ownerId,
        UserSummaryView owner,
        String courseId,
        String lessonId,
        String questionId,
        String title,
        String content,
        Double rating,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
