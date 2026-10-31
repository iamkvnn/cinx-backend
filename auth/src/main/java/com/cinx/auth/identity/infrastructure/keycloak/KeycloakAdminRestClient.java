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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
@ConditionalOnProperty(name = "security.keycloak.enabled", havingValue = "true")
public class KeycloakAdminRestClient implements KeycloakAdminClient {
    private static final Set<String> CINX_ROLES = java.util.Arrays.stream(AuthorizationRole.values())
            .map(Enum::name).collect(java.util.stream.Collectors.toUnmodifiableSet());

    private static final int PAGE_SIZE = 100;

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
    public Optional<KeycloakUser> findUser(String subject) {
        try {
            Map<String, Object> user = getUser(subject);
            return Optional.ofNullable(user).map(found -> new KeycloakUser(subject,
                    found.get("email") == null ? null : String.valueOf(found.get("email")),
                    Boolean.TRUE.equals(found.get("enabled"))));
        } catch (HttpClientErrorException.NotFound missing) {
            return Optional.empty();
        }
    }

    @Override
    public Set<AuthorizationRole> directRoles(String subject) {
        return cinxRoles(roleMappings("/admin/realms/{realm}/users/{subject}/role-mappings/realm", subject));
    }

    @Override
    public Set<AuthorizationRole> effectiveRoles(String subject) {
        return cinxRoles(roleMappings("/admin/realms/{realm}/users/{subject}/role-mappings/realm/composite", subject));
    }

    @Override
    public void addRoles(String subject, Set<AuthorizationRole> roles) {
        if (roles.isEmpty()) return;
        restClient.post()
                .uri("/admin/realms/{realm}/users/{subject}/role-mappings/realm", realm, subject)
                .headers(headers -> headers.setBearerAuth(accessToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(representations(roles))
                .retrieve()
                .toBodilessEntity();
    }

    @Override
    public void removeRoles(String subject, Set<AuthorizationRole> roles) {
        if (roles.isEmpty()) return;
        restClient.method(HttpMethod.DELETE)
                .uri("/admin/realms/{realm}/users/{subject}/role-mappings/realm", realm, subject)
                .headers(headers -> headers.setBearerAuth(accessToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(representations(roles))
                .retrieve()
                .toBodilessEntity();
    }

    @Override
    public void setEnabled(String subject, boolean enabled) {
        // Keycloak validates a PUT against the user profile (email and name are required), so send the whole user back
        Map<String, Object> user = getUser(subject);
        Map<String, Object> representation = user == null ? new LinkedHashMap<>() : new LinkedHashMap<>(user);
        representation.put("enabled", enabled);
        restClient.put()
                .uri("/admin/realms/{realm}/users/{subject}", realm, subject)
                .headers(headers -> headers.setBearerAuth(accessToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(representation)
                .retrieve()
                .toBodilessEntity();
    }

    @Override
    public List<String> enabledUsersWithRole(AuthorizationRole role) {
        List<String> result = new ArrayList<>();
        for (int first = 0; ; first += PAGE_SIZE) {
            List<Map<String, Object>> page = restClient.get()
                    .uri("/admin/realms/{realm}/roles/{role}/users?first={first}&max={max}", realm, role.name(), first, PAGE_SIZE)
                    .headers(headers -> headers.setBearerAuth(accessToken()))
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            if (page == null || page.isEmpty()) return result;
            page.stream().filter(user -> Boolean.TRUE.equals(user.get("enabled")))
                    .map(user -> String.valueOf(user.get("id"))).forEach(result::add);
            if (page.size() < PAGE_SIZE) return result;
        }
    }

    private Map<String, Object> getUser(String subject) {
        return restClient.get()
                .uri("/admin/realms/{realm}/users/{subject}", realm, subject)
                .headers(headers -> headers.setBearerAuth(accessToken()))
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {});
    }

    private List<Map<String, Object>> roleMappings(String uri, String subject) {
        List<Map<String, Object>> roles = restClient.get()
                .uri(uri, realm, subject)
                .headers(headers -> headers.setBearerAuth(accessToken()))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
        return roles == null ? List.of() : roles;
    }

    private Set<AuthorizationRole> cinxRoles(List<Map<String, Object>> mappings) {
        Set<AuthorizationRole> roles = EnumSet.noneOf(AuthorizationRole.class);
        mappings.stream().map(value -> String.valueOf(value.get("name"))).filter(CINX_ROLES::contains)
                .map(AuthorizationRole::valueOf).forEach(roles::add);
        return roles;
    }

    private List<Map<String, Object>> representations(Set<AuthorizationRole> roles) {
        return roles.stream().map(role -> restClient.get()
                        .uri("/admin/realms/{realm}/roles/{role}", realm, role.name())
                        .headers(headers -> headers.setBearerAuth(accessToken()))
                        .retrieve().body(new ParameterizedTypeReference<Map<String, Object>>() {}))
                .filter(java.util.Objects::nonNull).toList();
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
