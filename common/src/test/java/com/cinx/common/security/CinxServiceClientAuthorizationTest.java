package com.cinx.common.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CinxServiceClientAuthorizationTest {
    private final CinxServiceClientAuthorization authorization = new CinxServiceClientAuthorization();

    @Test
    void acceptsOnlyTheAuthorizedPartyInTheAllowlist() {
        JwtAuthenticationToken authentication = authentication("cinx-notification-service");

        assertThat(authorization.is(authentication, "cinx-notification-service")).isTrue();
        assertThat(authorization.is(authentication, "cinx-reporting-service")).isFalse();
    }

    private JwtAuthenticationToken authentication(String clientId) {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("service-account")
                .claim("azp", clientId)
                .build();
        return new JwtAuthenticationToken(jwt, List.of());
    }
}
