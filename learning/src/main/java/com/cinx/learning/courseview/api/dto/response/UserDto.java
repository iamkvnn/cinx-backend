package com.cinx.learning.courseview.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record UserDto(
        @Schema(example = "user_123") String userId, 
        @Schema(example = "John Doe") String name, 
        @Schema(example = "johndoe@example.com") String email, 
        @Schema(example = "https://example.com/avatar.jpg") String avatarUrl
) {
}