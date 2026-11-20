package com.cinx.learning.catalog.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/** What a course's instructor may see about someone taking part in that course: no email, no contact data. */
public record LearnerSummaryResponse(
        @Schema(example = "user_123")
        String userId,
        @Schema(example = "John Doe")
        String name,
        @Schema(example = "https://example.com/avatar.jpg")
        String avatarUrl
) {
}
