package com.cinx.common.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CinxJwtValidatorsTest {
    private static final Instant NOW = Instant.parse("2026-08-01T00:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Test
    void requiresExactTokenKindAndAllowedClient() {
        Jwt jwt = jwt(Map.of("token_kind", "service", "azp", "cinx-notification-service"));

        assertThat(CinxJwtValidators.tokenKind("service").validate(jwt).hasErrors()).isFalse();
        assertThat(CinxJwtValidators.tokenKind("user").validate(jwt).hasErrors()).isTrue();
        assertThat(CinxJwtValidators.authorizedParty(Set.of("cinx-notification-service"))
                .validate(jwt).hasErrors()).isFalse();
    }

    @Test
    void rejectsAtAuthorizationDeadlineAndMalformedClaim() {
        assertThat(CinxJwtValidators.authorizationNotAfter(CLOCK)
                .validate(jwt(Map.of("authz_not_after", NOW.getEpochSecond()))).hasErrors()).isTrue();
        assertThat(CinxJwtValidators.authorizationNotAfter(CLOCK)
                .validate(jwt(Map.of("authz_not_after", NOW.plusSeconds(1).toString()))).hasErrors()).isFalse();
        assertThat(CinxJwtValidators.authorizationNotAfter(CLOCK)
                .validate(jwt(Map.of("authz_not_after", "bad"))).hasErrors()).isTrue();
    }

    private Jwt jwt(Map<String, Object> claims) {
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("subject")
                .issuedAt(NOW.minusSeconds(10))
                .expiresAt(NOW.plusSeconds(60))
                .claims(values -> values.putAll(claims))
                .build();
    }
}
