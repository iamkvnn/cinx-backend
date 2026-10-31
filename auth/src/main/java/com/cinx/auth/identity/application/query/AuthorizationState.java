package com.cinx.auth.identity.application.query;

import com.cinx.auth.identity.domain.IdentityStatus;

import java.time.Instant;

/** Whether a user's tokens are still good: a token issued before {@code changedAt} is stale; null means none is. */
public record AuthorizationState(
        String userId,
        IdentityStatus status,
        Instant changedAt,
        Instant evaluatedAt) {
}
