package com.cinx.auth.identity.application.query;

import com.cinx.auth.identity.domain.IdentityStatus;

import java.time.Instant;
import java.util.List;

/** What an account may do at {@code evaluatedAt}, with the versions other services compare against. */
public record AuthorizationState(
        String userId,
        IdentityStatus status,
        List<String> roles,
        long authorizationVersion,
        long authorizationPolicyVersion,
        Instant evaluatedAt) {
}
