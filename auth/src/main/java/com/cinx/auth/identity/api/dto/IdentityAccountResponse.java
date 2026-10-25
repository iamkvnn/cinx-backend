package com.cinx.auth.identity.api.dto;

import com.cinx.auth.identity.domain.IdentityStatus;
import com.cinx.auth.identity.domain.IdentitySyncStatus;

import java.time.LocalDateTime;

public record IdentityAccountResponse(
        String userId,
        String keycloakSubject,
        String email,
        IdentityStatus status,
        IdentitySyncStatus syncStatus,
        LocalDateTime banExpiresAt
) {
}
