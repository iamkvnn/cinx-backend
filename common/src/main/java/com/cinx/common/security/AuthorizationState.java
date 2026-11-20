package com.cinx.common.security;

import java.time.Instant;

/** Auth's view of a user: their status, and the last authorization change ({@code changedAt}, null when none). */
public record AuthorizationState(String userId, String status, Instant changedAt, Instant evaluatedAt) {
}
