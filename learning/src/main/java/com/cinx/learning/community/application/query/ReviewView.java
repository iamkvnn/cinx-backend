package com.cinx.learning.community.application.query;

import java.time.LocalDateTime;
import java.util.List;

public record ReviewView(
        String id,
        String userId,
        UserSummaryView user,
        String courseId,
        String content,
        Double rating,
        ReviewReplyView reply,
        List<ReviewReactionView> reactions,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
