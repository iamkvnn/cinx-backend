package com.cinx.common.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class AuthorizationFreshnessVerifierTest {
    private static final Instant CHANGE = Instant.parse("2026-08-01T00:00:00.400Z");

    @Test
    void rejectsTokensIssuedBeforeTheLastChangeAndInactiveUsersAndRefreshesWithinFiveSecondBound() {
        MutableClock clock = new MutableClock(Instant.parse("2026-08-01T00:00:10Z"));
        AtomicInteger calls = new AtomicInteger();
        AuthorizationFreshnessVerifier verifier = new AuthorizationFreshnessVerifier(user -> {
            int call = calls.incrementAndGet();
            return new AuthorizationState(user, call == 1 ? "ACTIVE" : "BANNED", CHANGE, clock.instant());
        }, clock, Duration.ofSeconds(5));

        assertThat(verifier.isCurrent(jwt(CHANGE.minusSeconds(1)))).isFalse();
        assertThat(verifier.isCurrent(jwt(Instant.parse("2026-08-01T00:00:00Z")))).isTrue();
        assertThat(calls).hasValue(1);
        clock.advance(Duration.ofSeconds(5));
        assertThat(verifier.isCurrent(jwt(CHANGE.plusSeconds(1)))).isFalse();
        assertThat(calls).hasValue(2);
    }

    @Test
    void aUserAuthNeverChangedIsCurrentWithAnyToken() {
        AuthorizationFreshnessVerifier verifier = new AuthorizationFreshnessVerifier(
                user -> new AuthorizationState(user, "ACTIVE", null, Instant.now()), Clock.systemUTC(), Duration.ZERO);

        assertThat(verifier.isCurrent(jwt(Instant.parse("2020-01-01T00:00:00Z")))).isTrue();
    }

    @Test
    void failsClosedWhenRehydrationIsUnavailable() {
        AuthorizationFreshnessVerifier verifier = new AuthorizationFreshnessVerifier(user -> {
            throw new IllegalStateException("auth unavailable");
        }, Clock.systemUTC(), Duration.ofSeconds(5));

        assertThat(verifier.isCurrent(jwt(Instant.now()))).isFalse();
    }

    private Jwt jwt(Instant issuedAt) {
        return Jwt.withTokenValue("token").header("alg", "RS256").subject("user-1").issuedAt(issuedAt).build();
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
