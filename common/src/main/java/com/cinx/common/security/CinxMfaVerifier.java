package com.cinx.common.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Set;

/**
 * Evaluates the step-up authentication claims required by sensitive matrix
 * operations.  MFA is an authentication property; it is deliberately kept
 * separate from role and permission evaluation.
 */
@Component("cinxMfaVerifier")
public class CinxMfaVerifier {
    public static final String REQUIRED_ACR = "urn:cinx:loa:2";
    private static final Duration MAX_AGE = Duration.ofMinutes(15);
    private static final Set<String> APPROVED_METHODS = Set.of("otp", "hwk", "webauthn");

    private final Clock clock;

    public CinxMfaVerifier() {
        this(Clock.systemUTC());
    }

    CinxMfaVerifier(Clock clock) {
        this.clock = clock;
    }

    public boolean isSatisfied(Authentication authentication) {
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)
                || !authentication.isAuthenticated()) {
            return false;
        }

        Jwt jwt = jwtAuthentication.getToken();
        if (!REQUIRED_ACR.equals(jwt.getClaimAsString("acr"))) {
            return false;
        }
        if (!hasApprovedMethod(jwt.getClaims().get("amr"))) {
            return false;
        }

        Instant authTime = instant(jwt.getClaims().get("auth_time"));
        Instant now = clock.instant();
        return authTime != null
                && !authTime.isAfter(now)
                && !authTime.isBefore(now.minus(MAX_AGE));
    }

    public boolean requireSatisfied(Authentication authentication) {
        if (!isSatisfied(authentication)) {
            throw new MfaStepUpRequiredException();
        }
        return true;
    }

    private boolean hasApprovedMethod(Object claim) {
        if (claim instanceof Collection<?> values) {
            return values.stream().map(String::valueOf).anyMatch(APPROVED_METHODS::contains);
        }
        return claim instanceof String value && APPROVED_METHODS.contains(value);
    }

    private Instant instant(Object claim) {
        if (claim instanceof Number number) {
            return Instant.ofEpochSecond(number.longValue());
        }
        if (claim instanceof String value) {
            try {
                return value.chars().allMatch(Character::isDigit)
                        ? Instant.ofEpochSecond(Long.parseLong(value))
                        : Instant.parse(value);
            } catch (RuntimeException ignored) {
                return null;
            }
        }
        return null;
    }
}
