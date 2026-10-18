package com.cinx.common.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.*;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class AuthorizationFreshnessVerifierTest {
    @Test
    void rejectsInactiveAndOlderTokenVersionAndRefreshesWithinFiveSecondBound() {
        MutableClock clock = new MutableClock(Instant.parse("2026-08-01T00:00:00Z"));
        AtomicInteger calls = new AtomicInteger();
        AuthorizationFreshnessVerifier verifier = new AuthorizationFreshnessVerifier(user -> {
            int call = calls.incrementAndGet();
            return new AuthorizationState(user, call == 1 ? "ACTIVE" : "BANNED", call, 1, clock.instant());
        }, clock, Duration.ofSeconds(5));

        assertThat(verifier.isCurrent(jwt(1))).isTrue();
        assertThat(verifier.isCurrent(jwt(0))).isFalse();
        clock.advance(Duration.ofSeconds(5));
        assertThat(verifier.isCurrent(jwt(2))).isFalse();
    }

    @Test
    void failsClosedWhenRehydrationIsUnavailable() {
        AuthorizationFreshnessVerifier verifier = new AuthorizationFreshnessVerifier(user -> {
            throw new IllegalStateException("auth unavailable");
        }, Clock.systemUTC(), Duration.ofSeconds(5));

        assertThat(verifier.isCurrent(jwt(1))).isFalse();
    }

    private Jwt jwt(long version) {
        return Jwt.withTokenValue("token").header("alg", "RS256").subject("subject")
                .claims(claims -> claims.putAll(Map.of("uid", "user-1", "authz_ver", version)))
                .build();
    }

    private static final class MutableClock extends Clock {
        private Instant now;
        private MutableClock(Instant now) { this.now = now; }
        void advance(Duration duration) { now = now.plus(duration); }
        public ZoneId getZone() { return ZoneOffset.UTC; }
        public Clock withZone(ZoneId zone) { return this; }
        public Instant instant() { return now; }
    }
}
