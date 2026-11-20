package com.cinx.common.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Set;

public final class CinxJwtValidators {
    private static final OAuth2Error INVALID_KIND = new OAuth2Error(
            "invalid_token", "Unexpected or missing token_kind", null);
    private static final OAuth2Error INVALID_CLIENT = new OAuth2Error(
            "invalid_token", "Token client is not allowed", null);

    private CinxJwtValidators() {
    }

    public static OAuth2TokenValidator<Jwt> tokenKind(String expected) {
        return jwt -> expected.equals(jwt.getClaimAsString("token_kind"))
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(INVALID_KIND);
    }

    public static OAuth2TokenValidator<Jwt> authorizedParty(Set<String> allowedClients) {
        Set<String> allowlist = Set.copyOf(allowedClients);
        return jwt -> allowlist.contains(jwt.getClaimAsString("azp"))
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(INVALID_CLIENT);
    }
}
