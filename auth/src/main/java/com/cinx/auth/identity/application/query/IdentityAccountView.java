package com.cinx.auth.identity.application.query;

import com.cinx.auth.identity.domain.IdentityStatus;

import java.time.LocalDateTime;
import java.util.List;

/** An account as an admin sees it: Keycloak's email and roles, auth's status and ban. */
public record IdentityAccountView(
        String userId,
        String email,
        IdentityStatus status,
        List<String> roles,
        LocalDateTime banExpiresAt,
        LocalDateTime changedAt) {
}
