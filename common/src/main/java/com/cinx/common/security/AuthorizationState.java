package com.cinx.common.security;

import java.time.Instant;

public record AuthorizationState(String userId, String status, long authorizationVersion,
                                 long authorizationPolicyVersion, Instant evaluatedAt) {
}
