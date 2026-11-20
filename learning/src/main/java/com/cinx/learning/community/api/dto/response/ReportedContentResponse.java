package com.cinx.learning.community.api.dto.response;

import com.cinx.learning.community.domain.ReportType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record ReportedContentResponse(
        @Schema(example = "review_123")
        String id,
        ReportType type,
        @Schema(example = "user_123")
        String ownerId,
        UserSummaryResponse owner,
        @Schema(example = "course_123")
        String courseId,
        @Schema(example = "lesson_123")
        String lessonId,
        @Schema(example = "question_123")
        String questionId,
        @Schema(example = "How does polymorphism work?")
        String title,
        @Schema(example = "Contains the reported content body.")
        String content,
        @Schema(example = "4.5")
        Double rating,
        @Schema(example = "2025-01-01T10:00:00")
        LocalDateTime createdAt,
        @Schema(example = "2025-01-01T10:00:00")
        LocalDateTime updatedAt
) {
}
