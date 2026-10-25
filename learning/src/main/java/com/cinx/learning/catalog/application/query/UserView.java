package com.cinx.learning.catalog.application.query;

import com.cinx.learning.catalog.domain.Gender;

import java.time.LocalDateTime;

public record UserView(
        String userId, 
        String name, 
        String email, 
        Gender gender, 
        String avatarUrl,
        String bio,
        LocalDateTime createdAt
) {
}

