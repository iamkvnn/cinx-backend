package com.cinx.common.security;

import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

public final class AuthorizationFreshnessVerifier {
    private final AuthorizationStateSource source;
    private final Clock clock;
    private final Duration maximumAge;
    private final ConcurrentHashMap<String, Cached> cache = new ConcurrentHashMap<>();

    public AuthorizationFreshnessVerifier(AuthorizationStateSource source, Clock clock, Duration maximumAge) {
        if (maximumAge.isNegative() || maximumAge.compareTo(Duration.ofSeconds(5)) > 0) {
            throw new IllegalArgumentException("Authorization state maximum age must be between zero and five seconds");
        }
        this.source = source; this.clock = clock; this.maximumAge = maximumAge;
    }

    public boolean isCurrent(Jwt jwt) {
        String userId = jwt.getSubject();
        if (userId == null || userId.isBlank() || jwt.getIssuedAt() == null) return false;
        Instant now = clock.instant();
        Cached cached = cache.get(userId);
        if (cached == null || !now.isBefore(cached.loadedAt.plus(maximumAge))) {
            AuthorizationState loaded;
            try {
                loaded = source.load(userId);
            } catch (RuntimeException unavailable) {
                return false;
            }
            if (loaded == null || !userId.equals(loaded.userId())) return false;
            cached = new Cached(loaded, now);
            cache.put(userId, cached);
        }
        return "ACTIVE".equals(cached.state.status())
                && TokenFreshness.issuedAfterChange(jwt.getIssuedAt(), cached.state.changedAt());
    }

    public void acceptSnapshot(AuthorizationState state) {
        cache.compute(state.userId(), (ignored, existing) -> existing == null || isNewer(state, existing.state)
                ? new Cached(state, clock.instant()) : existing);
    }

    private static boolean isNewer(AuthorizationState candidate, AuthorizationState current) {
        if (candidate.changedAt() == null) return false;
        return current.changedAt() == null || candidate.changedAt().isAfter(current.changedAt());
    }

    private record Cached(AuthorizationState state, Instant loadedAt) {}
}
