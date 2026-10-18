package com.cinx.gateway.ratelimit;

import com.cinx.gateway.config.GatewayProblemDetailWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Per-minute limit on the course search, the one anonymous route that costs an embedding per call (P1-01). A guest is
 * counted by an HMAC of their IP, never the IP itself, and a signed-in user by uid; keys live one window. When Redis
 * fails the request passes: search staying up matters more than the limit.
 */
@Component
public class SearchRateLimitFilter implements GlobalFilter, Ordered {
    static final String PATH = "/api/v1/recommendations/search";
    private static final Logger log = LoggerFactory.getLogger(SearchRateLimitFilter.class);
    private static final long WINDOW_MS = 60_000;

    private final ReactiveStringRedisTemplate redis;
    private final int guestPerMinute;
    private final int userPerMinute;
    private final boolean trustForwardedFor;
    private final byte[] key;
    private final Clock clock;
    // epoch, not Long.MIN_VALUE: now minus that would overflow and the first warning would never come
    private final AtomicLong lastWarning = new AtomicLong(0);

    @Autowired
    public SearchRateLimitFilter(ReactiveStringRedisTemplate redis,
                                 @Value("${cinx.rate-limit.search.guest-per-minute:30}") int guestPerMinute,
                                 @Value("${cinx.rate-limit.search.user-per-minute:120}") int userPerMinute,
                                 @Value("${cinx.rate-limit.trust-forwarded-for:false}") boolean trustForwardedFor,
                                 @Value("${cinx.rate-limit.key:}") String key) {
        this(redis, guestPerMinute, userPerMinute, trustForwardedFor, key, Clock.systemUTC());
    }

    SearchRateLimitFilter(ReactiveStringRedisTemplate redis, int guestPerMinute, int userPerMinute,
                          boolean trustForwardedFor, String key, Clock clock) {
        this.redis = redis;
        this.guestPerMinute = guestPerMinute;
        this.userPerMinute = userPerMinute;
        this.trustForwardedFor = trustForwardedFor;
        this.clock = clock;
        if (key == null || key.isBlank()) {
            // still no raw IP in Redis; but instances no longer share guest counts, so set the key per environment
            log.warn("cinx.rate-limit.key is not set; guests are counted with a key made for this instance only");
            byte[] random = new byte[32];
            new SecureRandom().nextBytes(random);
            this.key = random;
        } else {
            this.key = key.getBytes(StandardCharsets.UTF_8);
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        if (request.getMethod() != HttpMethod.GET || !PATH.equals(request.getPath().value())) {
            return chain.filter(exchange);
        }
        long now = clock.millis();
        long window = now / WINDOW_MS;
        return exchange.getPrincipal().map(Optional::of).defaultIfEmpty(Optional.empty())
                .flatMap(principal -> {
                    String subject = principal.filter(JwtAuthenticationToken.class::isInstance)
                            .map(p -> "u:" + userId((JwtAuthenticationToken) p))
                            .orElseGet(() -> "ip:" + hmac(clientIp(request)));
                    int limit = subject.startsWith("u:") ? userPerMinute : guestPerMinute;
                    String counter = "rl:search:" + subject + ":" + window;
                    // the decision is made first and errors stop there, so the request itself never runs twice
                    return count(counter)
                            .map(used -> new Decision(limit, used))
                            .onErrorResume(failure -> {
                                warnAtMostOncePerMinute(now, failure);
                                return Mono.just(new Decision(limit, -1));
                            });
                })
                .flatMap(decision -> {
                    if (decision.used() < 0) return chain.filter(exchange);
                    exchange.getResponse().getHeaders().set("X-RateLimit-Limit", String.valueOf(decision.limit()));
                    exchange.getResponse().getHeaders().set("X-RateLimit-Remaining", String.valueOf(Math.max(0, decision.limit() - decision.used())));
                    if (decision.used() <= decision.limit()) return chain.filter(exchange);
                    long retryAfter = Math.max(1, (WINDOW_MS - now % WINDOW_MS + 999) / 1000);
                    exchange.getResponse().getHeaders().set("Retry-After", String.valueOf(retryAfter));
                    return GatewayProblemDetailWriter.write(exchange, HttpStatus.TOO_MANY_REQUESTS, "QUOTA_EXCEEDED",
                            "Too many searches", "Too many searches this minute; try again in " + retryAfter + " seconds");
                });
    }

    private Mono<Long> count(String counter) {
        return redis.opsForValue().increment(counter)
                .flatMap(used -> used == 1
                        ? redis.expire(counter, Duration.ofMillis(WINDOW_MS)).thenReturn(used)
                        : Mono.just(used));
    }

    private String clientIp(ServerHttpRequest request) {
        if (trustForwardedFor) {
            String forwarded = request.getHeaders().getFirst("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) return forwarded.split(",")[0].trim();
        }
        InetSocketAddress remote = request.getRemoteAddress();
        return remote == null || remote.getAddress() == null ? "unknown" : remote.getAddress().getHostAddress();
    }

    private String hmac(String ip) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(ip.getBytes(StandardCharsets.UTF_8)), 0, 16);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA256 is missing from the JVM", e);
        }
    }

    private static String userId(JwtAuthenticationToken authentication) {
        String uid = authentication.getToken().getClaimAsString("uid");
        return uid == null || uid.isBlank() ? authentication.getToken().getSubject() : uid;
    }

    private void warnAtMostOncePerMinute(long now, Throwable failure) {
        long last = lastWarning.get();
        if (now - last >= WINDOW_MS && lastWarning.compareAndSet(last, now)) {
            log.warn("Search rate limit skipped: Redis is unavailable ({})", failure.toString());
        }
    }

    private record Decision(int limit, long used) {
    }
}
