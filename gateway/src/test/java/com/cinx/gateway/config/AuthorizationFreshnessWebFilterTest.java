package com.cinx.gateway.config;

import com.cinx.common.security.AuthorizationChangeKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthorizationFreshnessWebFilterTest {
    private static final Instant CHANGE = Instant.parse("2026-11-14T10:00:00.700Z");

    private final AtomicInteger passed = new AtomicInteger();
    private final WebFilterChain chain = exchange -> {
        passed.incrementAndGet();
        return Mono.empty();
    };
    private ReactiveValueOperations<String, String> values;
    private AuthorizationFreshnessWebFilter filter;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        ReactiveStringRedisTemplate redis = mock(ReactiveStringRedisTemplate.class);
        values = mock(ReactiveValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.get(anyString())).thenReturn(Mono.empty());
        filter = new AuthorizationFreshnessWebFilter(redis, Clock.fixed(CHANGE, ZoneOffset.UTC));
    }

    @Test
    void aGuestIsNotChecked() {
        ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/courses"));

        run(exchange);

        assertThat(passed).hasValue(1);
        verify(values, never()).get(anyString());
    }

    @Test
    void aUserWithNoRecentChangePasses() {
        run(user("u1", "2026-11-14T09:58:00Z"));

        assertThat(passed).hasValue(1);
        verify(values).get("authz:changed:u1");
    }

    @Test
    void aTokenIssuedBeforeTheChangeIsStale() {
        changed("u1", "ACTIVE");
        ServerWebExchange exchange = user("u1", "2026-11-14T09:59:59Z");

        run(exchange);

        assertThat(passed).hasValue(0);
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void aTokenRenewedAfterTheChangePasses() {
        changed("u1", "ACTIVE");

        run(user("u1", "2026-11-14T10:00:00Z"));

        assertThat(passed).hasValue(1);
    }

    @Test
    void aBannedUserIsRefusedWhateverTheTokenAge() {
        changed("u1", "BANNED");
        ServerWebExchange exchange = user("u1", "2026-11-14T10:01:00Z");

        run(exchange);

        assertThat(passed).hasValue(0);
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void redisDownLetsTheRequestThrough() {
        when(values.get(anyString())).thenReturn(Mono.error(new RedisConnectionFailureException("down")));

        run(user("u1", "2026-11-14T09:00:00Z"));

        assertThat(passed).hasValue(1);
    }

    @Test
    void aSlowRedisLetsTheRequestThrough() {
        when(values.get(anyString())).thenReturn(Mono.never());

        run(user("u1", "2026-11-14T09:00:00Z"));

        assertThat(passed).hasValue(1);
    }

    @Test
    void anUnreadableValueLetsTheRequestThrough() {
        when(values.get(anyString())).thenReturn(Mono.just("garbage"));

        run(user("u1", "2026-11-14T09:00:00Z"));

        assertThat(passed).hasValue(1);
    }

    @Test
    void aTokenWithoutIssuedAtIsStale() {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none").subject("u1").build();
        ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/orders"))
                .mutate().principal(Mono.just(new JwtAuthenticationToken(jwt))).build();

        run(exchange);

        assertThat(passed).hasValue(0);
    }

    @Test
    void aFailureDownTheChainIsNotSwallowed() {
        WebFilterChain failing = exchange -> Mono.error(new IllegalStateException("downstream"));

        Mono<Void> result = filter.filter(user("u1", "2026-11-14T09:58:00Z"), failing);

        assertThatThrownBy(result::block).hasMessageContaining("downstream");
    }

    private void changed(String userId, String status) {
        when(values.get(AuthorizationChangeKey.key(userId)))
                .thenReturn(Mono.just(AuthorizationChangeKey.encode(status, CHANGE)));
    }

    private void run(ServerWebExchange exchange) {
        filter.filter(exchange, chain).block();
    }

    private static ServerWebExchange user(String sub, String issuedAt) {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none").subject(sub).issuedAt(Instant.parse(issuedAt)).build();
        return MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/orders"))
                .mutate().principal(Mono.just(new JwtAuthenticationToken(jwt))).build();
    }
}
