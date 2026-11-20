package com.cinx.auth.identity.api.dto;

import com.cinx.auth.identity.domain.IdentityStatus;

import java.time.LocalDateTime;
import java.util.List;

public record IdentityAccountResponse(
        String userId,
        String email,
        IdentityStatus status,
        List<String> roles,
        LocalDateTime banExpiresAt,
        LocalDateTime changedAt
) {
}
