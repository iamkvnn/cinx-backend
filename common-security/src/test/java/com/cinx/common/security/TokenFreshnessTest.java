package com.cinx.common.security;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class TokenFreshnessTest {
    private static final Instant CHANGE = Instant.parse("2026-10-29T10:00:00.700Z");

    @Test
    void anyTokenIsFreshWhenNothingEverChanged() {
        assertThat(TokenFreshness.issuedAfterChange(Instant.parse("2020-01-01T00:00:00Z"), null)).isTrue();
    }

    @Test
    void aTokenFromTheSecondOfTheChangeIsFresh() {
        assertThat(TokenFreshness.issuedAfterChange(Instant.parse("2026-10-29T10:00:00Z"), CHANGE)).isTrue();
        assertThat(TokenFreshness.issuedAfterChange(Instant.parse("2026-10-29T10:00:01Z"), CHANGE)).isTrue();
    }

    @Test
    void aTokenFromAnEarlierSecondIsStale() {
        assertThat(TokenFreshness.issuedAfterChange(Instant.parse("2026-10-29T09:59:59Z"), CHANGE)).isFalse();
    }

    @Test
    void aTokenWithoutIssuedAtIsStaleOnceSomethingChanged() {
        assertThat(TokenFreshness.issuedAfterChange(null, CHANGE)).isFalse();
    }
}
