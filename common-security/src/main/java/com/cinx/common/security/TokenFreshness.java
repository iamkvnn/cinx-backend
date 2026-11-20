package com.cinx.common.security;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/** Whether a user token was issued after the last change to that user's authorization. */
public final class TokenFreshness {
    private TokenFreshness() {
    }

    /**
     * {@code iat} has whole seconds, so a token from the second of the change counts as fresh: a renew right after the
     * change must pass, because the web retries a 401 once only.
     *
     * @param changedAt the last change, or null when there never was one
     */
    public static boolean issuedAfterChange(Instant issuedAt, Instant changedAt) {
        if (changedAt == null) return true;
        if (issuedAt == null) return false;
        return !issuedAt.isBefore(changedAt.truncatedTo(ChronoUnit.SECONDS));
    }
}
