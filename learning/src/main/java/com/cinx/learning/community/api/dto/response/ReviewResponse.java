package com.cinx.learning.community.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

public record ReviewResponse(
        @Schema(example = "rev_123")
        String id,
        @Schema(example = "user_123")
        String userId,
        UserSummaryResponse user,
        @Schema(example = "course_123")
        String courseId,
        @Schema(example = "Great course, learned a lot!")
        String content,
        @Schema(example = "4.5")
        Double rating,
        ReviewReplyDto reply,
        List<ReviewReactionResponse> reactions,
        @Schema(example = "2025-01-01T10:00:00")
        LocalDateTime createdAt,
        @Schema(example = "2025-01-01T10:00:00")
        LocalDateTime updatedAt
) {
}
