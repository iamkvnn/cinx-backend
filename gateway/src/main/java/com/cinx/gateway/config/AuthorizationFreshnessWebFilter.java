package com.cinx.gateway.config;

import com.cinx.common.security.AuthorizationChangeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Refuses a user token issued before that user's last status or role change, which auth writes to Redis
 * ({@link AuthorizationChangeKey}). The services behind the gateway do not repeat this check. When Redis cannot answer
 * the request goes on, and a revocation waits for the token to expire instead.
 */
final class AuthorizationFreshnessWebFilter implements WebFilter {
    private static final Logger log = LoggerFactory.getLogger(AuthorizationFreshnessWebFilter.class);
    static final Duration READ_TIMEOUT = Duration.ofMillis(50);
    private static final long WARN_EVERY_MS = 60_000;

    private final ReactiveStringRedisTemplate redis;
    private final Clock clock;
    private final AtomicLong lastWarning = new AtomicLong(Long.MIN_VALUE / 2);

    AuthorizationFreshnessWebFilter(ReactiveStringRedisTemplate redis, Clock clock) {
        this.redis = redis;
        this.clock = clock;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        // the verdict is settled first, so a failure further down the chain is never mistaken for a Redis failure
        return exchange.getPrincipal()
                .filter(JwtAuthenticationToken.class::isInstance)
                .flatMap(principal -> admits(((JwtAuthenticationToken) principal).getToken()))
                .defaultIfEmpty(true)
                .flatMap(admitted -> admitted ? chain.filter(exchange) : stale(exchange));
    }

    private Mono<Boolean> admits(Jwt jwt) {
        String userId = jwt.getSubject();
        Instant issuedAt = jwt.getIssuedAt();
        if (userId == null || userId.isBlank() || issuedAt == null) return Mono.just(false);
        return redis.opsForValue().get(AuthorizationChangeKey.key(userId))
                .timeout(READ_TIMEOUT)
                .map(value -> {
                    AuthorizationChangeKey.Change change = AuthorizationChangeKey.decode(value);
                    if (change == null) {
                        warnAtMostOncePerMinute("an unreadable authorization-change value for userId=" + userId);
                        return true;
                    }
                    return change.admits(issuedAt);
                })
                .defaultIfEmpty(true)
                .onErrorResume(failure -> {
                    warnAtMostOncePerMinute("Redis is unavailable (" + failure + ")");
                    return Mono.just(true);
                });
    }

    private Mono<Void> stale(ServerWebExchange exchange) {
        return GatewayProblemDetailWriter.write(exchange, HttpStatus.UNAUTHORIZED,
                "TOKEN_AUTHORIZATION_STALE", "Unauthorized", "Token authorization state is stale or inactive");
    }

    private void warnAtMostOncePerMinute(String reason) {
        long now = clock.millis();
        long last = lastWarning.get();
        if (now - last >= WARN_EVERY_MS && lastWarning.compareAndSet(last, now)) {
            log.warn("Token freshness not checked: {}", reason);
        }
    }
}
