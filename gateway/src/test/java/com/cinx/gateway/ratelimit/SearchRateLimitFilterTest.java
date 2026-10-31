package com.cinx.gateway.ratelimit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SearchRateLimitFilterTest {
    // 20 s into a minute: a refused caller waits 40 s
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-13T09:00:20Z"), ZoneOffset.UTC);

    private final Map<String, Long> counters = new HashMap<>();
    private final AtomicInteger passed = new AtomicInteger();
    private final GatewayFilterChain chain = exchange -> {
        passed.incrementAndGet();
        return Mono.empty();
    };
    private ReactiveStringRedisTemplate redis;
    private SearchRateLimitFilter filter;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redis = mock(ReactiveStringRedisTemplate.class);
        ReactiveValueOperations<String, String> values = mock(ReactiveValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.increment(anyString())).thenAnswer(call -> Mono.just(counters.merge(call.getArgument(0), 1L, Long::sum)));
        when(redis.expire(anyString(), any(Duration.class))).thenReturn(Mono.just(true));
        filter = new SearchRateLimitFilter(redis, 3, 5, false, "test-key", CLOCK);
    }

    @Test
    void aGuestGetsTheGuestLimitThen429WithRetryAfter() {
        for (int i = 0; i < 3; i++) run(guest("203.0.113.7"));
        ServerWebExchange refused = guest("203.0.113.7");
        run(refused);

        assertThat(passed).hasValue(3);
        assertThat(refused.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(refused.getResponse().getHeaders().getFirst("Retry-After")).isEqualTo("40");
        assertThat(refused.getResponse().getHeaders().getFirst("X-RateLimit-Remaining")).isEqualTo("0");
    }

    @Test
    void guestsAreCountedApartAndTheKeyHoldsNoRawIp() {
        run(guest("203.0.113.7"));
        run(guest("198.51.100.9"));

        assertThat(counters).hasSize(2);
        assertThat(counters.keySet()).allSatisfy(key -> {
            assertThat(key).startsWith("rl:search:ip:").doesNotContain("203.0.113.7").doesNotContain("198.51.100.9");
        });
    }

    @Test
    void aSignedInUserIsCountedBySubWithTheUserLimit() {
        for (int i = 0; i < 6; i++) run(user("u1", "203.0.113.7"));

        assertThat(passed).hasValue(5);
        assertThat(counters.keySet()).singleElement().satisfies(key -> assertThat(key).startsWith("rl:search:u:u1:"));
    }

    @Test
    void forwardedForIsIgnoredUnlessTrusted() {
        run(guestForwarded("203.0.113.7", "1.1.1.1"));
        run(guestForwarded("203.0.113.7", "2.2.2.2"));
        assertThat(counters).hasSize(1);

        SearchRateLimitFilter trusting = new SearchRateLimitFilter(redis, 3, 5, true, "test-key", CLOCK);
        counters.clear();
        trusting.filter(guestForwarded("203.0.113.7", "1.1.1.1"), chain).block();
        trusting.filter(guestForwarded("203.0.113.7", "2.2.2.2, 10.0.0.1"), chain).block();
        assertThat(counters).hasSize(2);
    }

    @Test
    void redisDownLetsTheSearchThrough() {
        when(redis.opsForValue().increment(anyString())).thenReturn(Mono.error(new RedisConnectionFailureException("down")));

        ServerWebExchange exchange = guest("203.0.113.7");
        run(exchange);

        assertThat(passed).hasValue(1);
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    void otherRoutesAreNotCounted() {
        run(MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/courses").remoteAddress(address("203.0.113.7"))));
        run(MockServerWebExchange.from(MockServerHttpRequest.post(SearchRateLimitFilter.PATH).remoteAddress(address("203.0.113.7"))));

        assertThat(passed).hasValue(2);
        assertThat(counters).isEmpty();
    }

    private void run(ServerWebExchange exchange) {
        filter.filter(exchange, chain).block();
    }

    private static ServerWebExchange guest(String ip) {
        return MockServerWebExchange.from(MockServerHttpRequest.get(SearchRateLimitFilter.PATH + "?query=python").remoteAddress(address(ip)));
    }

    private static ServerWebExchange guestForwarded(String ip, String forwardedFor) {
        return MockServerWebExchange.from(MockServerHttpRequest.get(SearchRateLimitFilter.PATH)
                .remoteAddress(address(ip)).header("X-Forwarded-For", forwardedFor));
    }

    private static ServerWebExchange user(String sub, String ip) {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none").subject(sub).build();
        return guest(ip).mutate().principal(Mono.just(new JwtAuthenticationToken(jwt))).build();
    }

    private static InetSocketAddress address(String ip) {
        return new InetSocketAddress(ip, 50000);
    }
}
