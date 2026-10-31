package com.cinx.auth.identity.api.dto;

import com.cinx.auth.identity.domain.IdentityStatus;

import java.time.Instant;

public record AuthorizationStateResponse(
        String userId,
        IdentityStatus status,
        Instant changedAt,
        Instant evaluatedAt
) {
}
