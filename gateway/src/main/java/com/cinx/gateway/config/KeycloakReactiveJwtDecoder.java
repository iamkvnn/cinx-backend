package com.cinx.gateway.config;

import com.cinx.common.security.CinxJwtValidators;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;

import java.time.Clock;

final class KeycloakReactiveJwtDecoder {
    private KeycloakReactiveJwtDecoder() {
    }

    static ReactiveJwtDecoder create(String issuer, String jwkSetUri, String audience) {
        if (issuer == null || issuer.isBlank() || jwkSetUri == null || jwkSetUri.isBlank()
                || audience == null || audience.isBlank()) {
            throw new IllegalArgumentException("issuer-uri, jwk-set-uri and audience are required");
        }
        NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder.withJwkSetUri(jwkSetUri).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuer),
                new JwtClaimValidator<java.util.List<String>>("aud",
                        audiences -> audiences != null && audiences.contains(audience)),
                CinxJwtValidators.tokenKind("user"),
                CinxJwtValidators.authorizationNotAfter(Clock.systemUTC())));
        return decoder;
    }
}
