package com.cinx.common.security;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.Instant;

public final class ServiceAuthorizationStateSource implements AuthorizationStateSource {
    private final RestClient authClient;
    private final ServiceTokenProvider tokens;

    public ServiceAuthorizationStateSource(RestClient.Builder builder, String authBaseUrl, String tokenUri,
                                           String clientId, String clientSecret, String audience, Clock clock) {
        this.authClient = builder.baseUrl(authBaseUrl).build();
        this.tokens = new ServiceTokenProvider(builder.build(), tokenUri, clientId, clientSecret, audience, clock);
    }

    @Override
    public AuthorizationState load(String userId) {
        Response response = authClient.get()
                .uri("/internal/identity/accounts/{userId}/authorization-state", userId)
                .headers(headers -> headers.setBearerAuth(tokens.accessToken()))
                .retrieve().body(Response.class);
        return response == null ? null : response.data();
    }

    private record Response(boolean success, String message, AuthorizationState data) {}

    private static final class ServiceTokenProvider {
        private final RestClient client; private final String tokenUri; private final String clientId;
        private final String clientSecret; private final String audience;
        private final Clock clock; private volatile Token token;
        private ServiceTokenProvider(RestClient client, String tokenUri, String clientId, String clientSecret,
                                     String audience, Clock clock) {
            this.client = client; this.tokenUri = tokenUri; this.clientId = clientId;
            this.clientSecret = clientSecret; this.audience = audience; this.clock = clock;
        }
        private synchronized String accessToken() {
            if (token != null && clock.instant().isBefore(token.expiresAt.minusSeconds(10))) return token.value;
            if (clientSecret == null || clientSecret.isBlank()) throw new IllegalStateException("Service client secret is required");
            var form = new LinkedMultiValueMap<String, String>();
            form.add("grant_type", "client_credentials"); form.add("client_id", clientId);
            form.add("client_secret", clientSecret); form.add("audience", audience);
            TokenResponse response = client.post().uri(tokenUri).contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form).retrieve().body(TokenResponse.class);
            if (response == null || response.accessToken == null) throw new IllegalStateException("No service token returned");
            token = new Token(response.accessToken, clock.instant().plusSeconds(response.expiresIn));
            return token.value;
        }
    }
    private record Token(String value, Instant expiresAt) {}
    private record TokenResponse(@JsonProperty("access_token") String accessToken,
                                 @JsonProperty("expires_in") long expiresIn) {}
}
