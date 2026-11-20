package com.cinx.learning.community.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record ReviewReactionResponse(
        @Schema(example = "react_123")
        String id,
        @Schema(example = "user_123")
        String userId,
        UserSummaryResponse user,
        @Schema(example = "rev_123")
        String reviewId,
        @Schema(example = "true")
        boolean liked
) {
}
