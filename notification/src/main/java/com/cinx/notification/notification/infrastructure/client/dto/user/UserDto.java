package com.cinx.notification.notification.infrastructure.client.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;

public record UserDto(
        @Schema(example = "user_123") String userId, 
        @Schema(example = "John Doe") String name, 
        @Schema(example = "johndoe@example.com") String email
) {}