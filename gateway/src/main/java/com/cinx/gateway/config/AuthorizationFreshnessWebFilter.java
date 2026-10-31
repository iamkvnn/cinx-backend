package com.cinx.gateway.config;

import com.cinx.common.security.TokenFreshness;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.time.*;
import java.util.concurrent.ConcurrentHashMap;

final class AuthorizationFreshnessWebFilter implements WebFilter {
    private final WebClient client;
    private final String tokenUri, authBaseUrl, clientId, clientSecret, audience;
    private final Clock clock;
    private final ConcurrentHashMap<String, Cached> cache = new ConcurrentHashMap<>();

    AuthorizationFreshnessWebFilter(WebClient.Builder builder, String tokenUri, String authBaseUrl,
                                    String clientId, String clientSecret, String audience, Clock clock) {
        this.client = builder.build(); this.tokenUri = tokenUri; this.authBaseUrl = authBaseUrl;
        this.clientId = clientId; this.clientSecret = clientSecret; this.audience = audience; this.clock = clock;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return exchange.getPrincipal().map(java.util.Optional::of)
                .defaultIfEmpty(java.util.Optional.empty()).flatMap(optional -> {
            Object principal = optional.orElse(null);
            if (!(principal instanceof JwtAuthenticationToken authentication)) return chain.filter(exchange);
            String userId = authentication.getToken().getSubject();
            Instant issuedAt = authentication.getToken().getIssuedAt();
            if (userId == null || userId.isBlank() || issuedAt == null) return stale(exchange);
            String expectedUserId = userId;
            Cached cached = cache.get(userId);
            if (cached != null && clock.instant().isBefore(cached.loadedAt.plusSeconds(5))) {
                return allowed(userId, cached.state, issuedAt) ? chain.filter(exchange) : stale(exchange);
            }
            return load(expectedUserId).flatMap(state -> {
                cache.put(expectedUserId, new Cached(state, clock.instant()));
                return allowed(expectedUserId, state, issuedAt) ? chain.filter(exchange) : stale(exchange);
            }).onErrorResume(failure -> stale(exchange));
        });
    }

    private Mono<State> load(String userId) {
        if (clientSecret == null || clientSecret.isBlank()) return Mono.error(new IllegalStateException("Gateway service secret required"));
        var form = new LinkedMultiValueMap<String, String>();
        form.add("grant_type", "client_credentials"); form.add("client_id", clientId); form.add("client_secret", clientSecret);
        form.add("audience", audience);
        return client.post().uri(tokenUri).contentType(MediaType.APPLICATION_FORM_URLENCODED).bodyValue(form)
                .retrieve().bodyToMono(Token.class)
                .flatMap(token -> client.get().uri(authBaseUrl + "/internal/identity/accounts/{userId}/authorization-state", userId)
                        .headers(headers -> headers.setBearerAuth(token.accessToken))
                        .retrieve().bodyToMono(Response.class))
                .map(Response::data);
    }

    private boolean allowed(String userId, State state, Instant issuedAt) {
        return state != null && userId.equals(state.userId) && "ACTIVE".equals(state.status)
                && TokenFreshness.issuedAfterChange(issuedAt, state.changedAt);
    }

    private Mono<Void> stale(ServerWebExchange exchange) {
        return GatewayProblemDetailWriter.write(exchange, org.springframework.http.HttpStatus.UNAUTHORIZED,
                "TOKEN_AUTHORIZATION_STALE", "Unauthorized", "Token authorization state is stale or inactive");
    }

    private record Cached(State state, Instant loadedAt) {}
    private record Token(@JsonProperty("access_token") String accessToken) {}
    private record Response(boolean success, String message, State data) {}
    private record State(String userId, String status, Instant changedAt, Instant evaluatedAt) {}
}
