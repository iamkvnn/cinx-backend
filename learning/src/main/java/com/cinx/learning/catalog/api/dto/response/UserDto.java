package com.cinx.learning.catalog.api.dto.response;

import com.cinx.learning.catalog.domain.Gender;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record UserDto(
        @Schema(example = "usr_123")
        String userId, 
        @Schema(example = "Nguyen Van A")
        String name, 
        @Schema(example = "nguyenvana@gmail.com")
        String email, 
        @Schema(example = "MALE")
        Gender gender, 
        @Schema(example = "https://example.com/profiles/123.jpg")
        String avatarUrl,
        @Schema(example = "Tech Lead backend, 11 years.")
        String bio,
        @Schema(example = "2025-03-01T10:00:00")
        LocalDateTime createdAt
) {
}

