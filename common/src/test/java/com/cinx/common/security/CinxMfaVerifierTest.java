package com.cinx.common.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CinxMfaVerifierTest {
    private static final Instant NOW = Instant.parse("2026-08-01T00:00:00Z");
    private final CinxMfaVerifier verifier = new CinxMfaVerifier(Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void acceptsRecentApprovedStepUpClaims() {
        assertThat(verifier.isSatisfied(authentication(Map.of(
                "acr", CinxMfaVerifier.REQUIRED_ACR,
                "amr", List.of("pwd", "webauthn"),
                "auth_time", NOW.minusSeconds(60).getEpochSecond())))).isTrue();
    }

    @Test
    void rejectsWrongLevelMethodAndStaleClaims() {
        assertThat(verifier.isSatisfied(authentication(Map.of(
                "acr", "urn:cinx:loa:1", "amr", List.of("otp"),
                "auth_time", NOW.minusSeconds(60).getEpochSecond())))).isFalse();
        assertThat(verifier.isSatisfied(authentication(Map.of(
                "acr", CinxMfaVerifier.REQUIRED_ACR, "amr", List.of("pwd"),
                "auth_time", NOW.minusSeconds(60).getEpochSecond())))).isFalse();
        assertThat(verifier.isSatisfied(authentication(Map.of(
                "acr", CinxMfaVerifier.REQUIRED_ACR, "amr", List.of("otp"),
                "auth_time", NOW.minusSeconds(901).getEpochSecond())))).isFalse();
    }

    @Test
    void raisesTheStepUpErrorForInsufficientMfa() {
        assertThatThrownBy(() -> verifier.requireSatisfied(authentication(Map.of(
                "acr", "urn:cinx:loa:1", "amr", List.of("pwd"),
                "auth_time", NOW.minusSeconds(60).getEpochSecond()))))
                .isInstanceOf(MfaStepUpRequiredException.class);
    }

    private JwtAuthenticationToken authentication(Map<String, Object> claims) {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("user-1")
                .claims(values -> values.putAll(claims))
                .build();
        return new JwtAuthenticationToken(jwt, List.of());
    }
}
