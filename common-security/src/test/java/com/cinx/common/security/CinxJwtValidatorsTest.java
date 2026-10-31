package com.cinx.common.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CinxJwtValidatorsTest {
    private static final Instant NOW = Instant.parse("2026-08-01T00:00:00Z");

    @Test
    void requiresExactTokenKindAndAllowedClient() {
        Jwt jwt = jwt(Map.of("token_kind", "service", "azp", "cinx-notification-service"));

        assertThat(CinxJwtValidators.tokenKind("service").validate(jwt).hasErrors()).isFalse();
        assertThat(CinxJwtValidators.tokenKind("user").validate(jwt).hasErrors()).isTrue();
        assertThat(CinxJwtValidators.authorizedParty(Set.of("cinx-notification-service"))
                .validate(jwt).hasErrors()).isFalse();
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
