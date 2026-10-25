package com.cinx.auth.identity.api.dto;

import com.cinx.auth.identity.domain.IdentityStatus;

import java.time.Instant;
import java.util.List;

public record AuthorizationStateResponse(
        String userId,
        IdentityStatus status,
        List<String> roles,
        long authorizationVersion,
        long authorizationPolicyVersion,
        Instant evaluatedAt
) {
}
