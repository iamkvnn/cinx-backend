package com.cinx.learning.community.application.query;

public record ReviewReactionView(
        String id,
        String userId,
        UserSummaryView user,
        String reviewId,
        boolean liked
) {
}
