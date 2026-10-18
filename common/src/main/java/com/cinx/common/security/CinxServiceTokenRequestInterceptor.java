package com.cinx.common.security;

import com.fasterxml.jackson.annotation.JsonProperty;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.Instant;

/**
 * Adds a short-lived client-credentials token for an internal Feign call.
 * The requested audience is explicit so a service token cannot be replayed
 * against a different internal resource server.
 */
public final class CinxServiceTokenRequestInterceptor implements RequestInterceptor {
    private final RestClient tokenClient;
    private final String tokenUri;
    private final String clientId;
    private final String clientSecret;
    private final String audience;
    private final Clock clock;
    private volatile Token token;

    public CinxServiceTokenRequestInterceptor(RestClient.Builder builder, String tokenUri,
                                              String clientId, String clientSecret,
                                              String audience, Clock clock) {
        this.tokenClient = builder.build();
        this.tokenUri = requireText(tokenUri, "tokenUri");
        this.clientId = requireText(clientId, "clientId");
        this.clientSecret = clientSecret == null ? "" : clientSecret;
        this.audience = requireText(audience, "audience");
        this.clock = clock == null ? Clock.systemUTC() : clock;
    }

    @Override
    public void apply(RequestTemplate template) {
        template.header("Authorization", "Bearer " + accessToken());
    }

    private synchronized String accessToken() {
        if (token != null && clock.instant().isBefore(token.expiresAt().minusSeconds(10))) {
            return token.value();
        }
        if (clientSecret.isBlank()) {
            throw new IllegalStateException("Service client secret is required");
        }

        var form = new LinkedMultiValueMap<String, String>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("audience", audience);

        TokenResponse response = tokenClient.post()
                .uri(tokenUri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(TokenResponse.class);
        if (response == null || response.accessToken() == null || response.accessToken().isBlank()) {
            throw new IllegalStateException("No service token returned");
        }

        long lifetime = Math.max(response.expiresIn(), 1L);
        token = new Token(response.accessToken(), clock.instant().plusSeconds(lifetime));
        return token.value();
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value;
    }

    private record Token(String value, Instant expiresAt) {}

    private record TokenResponse(@JsonProperty("access_token") String accessToken,
                                 @JsonProperty("expires_in") long expiresIn) {}
}
