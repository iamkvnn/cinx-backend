package com.cinx.auth.identity.infrastructure.keycloak;

import com.cinx.auth.identity.application.KeycloakAdminClient;

import com.cinx.auth.identity.domain.AuthorizationRole;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@ConditionalOnProperty(name = "security.keycloak.enabled", havingValue = "true")
public class KeycloakAdminRestClient implements KeycloakAdminClient {
    private static final Set<String> CINX_ROLES = java.util.Arrays.stream(AuthorizationRole.values())
            .map(Enum::name).collect(java.util.stream.Collectors.toUnmodifiableSet());

    private final RestClient restClient;
    private final String realm;
    private final String clientId;
    private final String clientSecret;
    private volatile AdminToken cachedToken;

    KeycloakAdminRestClient(
            RestClient.Builder builder,
            @Value("${keycloak.base-url:http://localhost:8180}") String baseUrl,
            @Value("${keycloak.realm:cinx}") String realm,
            @Value("${keycloak.admin.client-id:cinx-auth-admin}") String clientId,
            @Value("${keycloak.admin.client-secret}") String clientSecret
    ) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.realm = realm;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    @Override
    public void synchronizeAuthorization(String keycloakSubject, String userId, boolean enabled,
                                         Set<AuthorizationRole> roles, long authorizationVersion,
                                         long policyVersion, Instant authorizationNotAfter) {
        Map<String, List<String>> attributes = new java.util.LinkedHashMap<>();
        attributes.put("uid", List.of(userId));
        attributes.put("authorization_version", List.of(String.valueOf(authorizationVersion)));
        attributes.put("authorization_policy_version", List.of(String.valueOf(policyVersion)));
        if (authorizationNotAfter != null) {
            attributes.put("authz_not_after", List.of(String.valueOf(authorizationNotAfter.getEpochSecond())));
        }
        // Keycloak validates a PUT against the user profile (email and name are required), so send the whole user back
        Map<String, Object> user = restClient.get()
                .uri("/admin/realms/{realm}/users/{subject}", realm, keycloakSubject)
                .headers(headers -> headers.setBearerAuth(accessToken()))
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {});
        Map<String, Object> representation = user == null ? new java.util.LinkedHashMap<>() : new java.util.LinkedHashMap<>(user);
        Map<String, Object> merged = new java.util.LinkedHashMap<>();
        if (representation.get("attributes") instanceof Map<?, ?> existing) {
            existing.forEach((key, value) -> merged.put(String.valueOf(key), value));
        }
        merged.putAll(attributes);
        representation.put("enabled", enabled);
        representation.put("attributes", merged);
        restClient.put()
                .uri("/admin/realms/{realm}/users/{subject}", realm, keycloakSubject)
                .headers(headers -> headers.setBearerAuth(accessToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(representation)
                .retrieve()
                .toBodilessEntity();
        replaceRoles(keycloakSubject, roles);
    }

    private void replaceRoles(String keycloakSubject, Set<AuthorizationRole> roles) {
        List<Map<String, Object>> currentRoles = currentRoles(keycloakSubject).stream()
                .filter(value -> CINX_ROLES.contains(String.valueOf(value.get("name"))))
                .toList();
        if (!currentRoles.isEmpty()) {
            restClient.method(HttpMethod.DELETE)
                    .uri("/admin/realms/{realm}/users/{subject}/role-mappings/realm", realm, keycloakSubject)
                    .headers(headers -> headers.setBearerAuth(accessToken()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(currentRoles)
                    .retrieve()
                    .toBodilessEntity();
        }

        List<Map<String, Object>> desiredRoles = roles.stream().map(role -> restClient.get()
                        .uri("/admin/realms/{realm}/roles/{role}", realm, role.name())
                        .headers(headers -> headers.setBearerAuth(accessToken()))
                        .retrieve().body(new ParameterizedTypeReference<Map<String, Object>>() {}))
                .filter(java.util.Objects::nonNull).toList();
        if (!desiredRoles.isEmpty()) {
            restClient.post()
                    .uri("/admin/realms/{realm}/users/{subject}/role-mappings/realm", realm, keycloakSubject)
                    .headers(headers -> headers.setBearerAuth(accessToken()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(desiredRoles)
                    .retrieve()
                    .toBodilessEntity();
        }
    }

    @Override
    public void revokeSessions(String keycloakSubject) {
        restClient.post()
                .uri("/admin/realms/{realm}/users/{subject}/logout", realm, keycloakSubject)
                .headers(headers -> headers.setBearerAuth(accessToken()))
                .retrieve()
                .toBodilessEntity();
    }

    private List<Map<String, Object>> currentRoles(String keycloakSubject) {
        List<Map<String, Object>> roles = restClient.get()
                .uri("/admin/realms/{realm}/users/{subject}/role-mappings/realm", realm, keycloakSubject)
                .headers(headers -> headers.setBearerAuth(accessToken()))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
        return roles == null ? List.of() : roles;
    }

    private String accessToken() {
        AdminToken token = cachedToken;
        if (token == null || token.expiresAt().isBefore(Instant.now().plusSeconds(10))) {
            synchronized (this) {
                token = cachedToken;
                if (token == null || token.expiresAt().isBefore(Instant.now().plusSeconds(10))) {
                    token = requestToken();
                    cachedToken = token;
                }
            }
        }
        return token.value();
    }

    private AdminToken requestToken() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        TokenResponse response = restClient.post()
                .uri("/realms/{realm}/protocol/openid-connect/token", realm)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(TokenResponse.class);
        if (response == null || response.accessToken() == null) {
            throw new IllegalStateException("Keycloak did not return an admin access token");
        }
        return new AdminToken(
                response.accessToken(),
                Instant.now().plusSeconds(Math.max(response.expiresIn() - 10L, 10L)));
    }

    private record TokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("expires_in") long expiresIn
    ) {
    }

    private record AdminToken(String value, Instant expiresAt) {
    }
}
