package com.cinx.learning.community.api.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UserSummaryResponse(
        @Schema(example = "user_123")
        String userId,
        @Schema(example = "John Doe")
        String name,
        @Schema(example = "johndoe@example.com")
        String email,
        @Schema(example = "STUDENT")
        String role,
        @Schema(example = "https://example.com/avatar.jpg")
        String avatarUrl
) {
    /** Public reads (the course page's review list) never carry an email. */
    public UserSummaryResponse withoutEmail() {
        return new UserSummaryResponse(userId, name, null, role, avatarUrl);
    }
}
