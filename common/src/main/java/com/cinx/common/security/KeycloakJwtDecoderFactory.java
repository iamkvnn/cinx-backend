package com.cinx.common.security;

import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.util.List;
import java.time.Clock;
import java.util.Set;

/**
 * Builds the final CINX resource-server decoder.
 *
 * <p>The issuer remains the public URL embedded in tokens while keys are loaded
 * from the internal Docker URL. This avoids issuer drift without requiring a
 * resource service to call Keycloak discovery.</p>
 */
public final class KeycloakJwtDecoderFactory {
    private KeycloakJwtDecoderFactory() {
    }

    public static JwtDecoder create(String issuer, String jwkSetUri, String audience) {
        return create(issuer, jwkSetUri, audience, "user", Set.of());
    }

    public static JwtDecoder create(String issuer, String jwkSetUri, String audience,
                                    String tokenKind, Set<String> allowedClients) {
        requireText(issuer, "security.keycloak.issuer-uri");
        requireText(jwkSetUri, "security.keycloak.jwk-set-uri");
        requireText(audience, "security.keycloak.audience");

        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();

        OAuth2TokenValidator<Jwt> issuerAndTime = JwtValidators.createDefaultWithIssuer(issuer);
        OAuth2TokenValidator<Jwt> audienceValidator = new JwtClaimValidator<List<String>>("aud",
                audiences -> audiences != null && audiences.contains(audience));
        java.util.List<OAuth2TokenValidator<Jwt>> validators = new java.util.ArrayList<>();
        validators.add(issuerAndTime);
        validators.add(audienceValidator);
        validators.add(CinxJwtValidators.tokenKind(tokenKind));
        validators.add(CinxJwtValidators.authorizationNotAfter(Clock.systemUTC()));
        if (!allowedClients.isEmpty()) validators.add(CinxJwtValidators.authorizedParty(allowedClients));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(validators));
        return decoder;
    }

    private static void requireText(String value, String property) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(property + " is required");
        }
    }
}
