package com.cinx.common.security;

import java.time.Duration;
import java.time.Instant;

/**
 * The Redis entry auth writes after a user's status or roles change, and the gateway reads on each request. It outlives
 * the realm's access-token lifespan (300 s), so once it expires every token issued before the change has expired too.
 */
public final class AuthorizationChangeKey {
    public static final Duration TTL = Duration.ofSeconds(360);
    private static final String PREFIX = "authz:changed:";
    private static final String ACTIVE = "ACTIVE";

    private AuthorizationChangeKey() {
    }

    public static String key(String userId) {
        if (userId == null || userId.isBlank()) throw new IllegalArgumentException("userId is required");
        return PREFIX + userId;
    }

    /**
     * {@code <STATUS> <changedAt in epoch milliseconds>}, e.g. {@code BANNED 1763114400700}. The time is a number so a
     * writer can compare it inside Redis and never replace a newer change with an older one.
     */
    public static String encode(String status, Instant changedAt) {
        if (status == null || status.isBlank() || status.contains(" ")) throw new IllegalArgumentException("status is invalid");
        if (changedAt == null) throw new IllegalArgumentException("changedAt is required");
        return status + " " + changedAt.toEpochMilli();
    }

    /** The change, or null when the value is not one {@link #encode} wrote. */
    public static Change decode(String value) {
        if (value == null) return null;
        int space = value.indexOf(' ');
        if (space <= 0 || space == value.length() - 1) return null;
        try {
            return new Change(value.substring(0, space), Instant.ofEpochMilli(Long.parseLong(value.substring(space + 1))));
        } catch (NumberFormatException malformed) {
            return null;
        }
    }

    public record Change(String status, Instant changedAt) {
        /** Whether a token issued at {@code issuedAt} may still be used after this change. */
        public boolean admits(Instant issuedAt) {
            return ACTIVE.equals(status) && TokenFreshness.issuedAfterChange(issuedAt, changedAt);
        }
    }
}
