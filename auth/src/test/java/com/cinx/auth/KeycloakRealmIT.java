package com.cinx.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

@Testcontainers
class KeycloakRealmIT {
    private static final String ADMIN_SECRET = "integration-admin-secret";
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    @Container
    static final GenericContainer<?> KEYCLOAK =
            new GenericContainer<>("quay.io/keycloak/keycloak:26.6.1")
                    .withEnv("KC_BOOTSTRAP_ADMIN_USERNAME", "admin")
                    .withEnv("KC_BOOTSTRAP_ADMIN_PASSWORD", "admin")
                    .withEnv("KEYCLOAK_ADMIN_CLIENT_SECRET", ADMIN_SECRET)
                    .withEnv("CINX_GATEWAY_SERVICE_CLIENT_SECRET", "integration-gateway-secret")
                    .withEnv("CINX_USER_SERVICE_CLIENT_SECRET", "integration-user-secret")
                    .withEnv("CINX_COMMERCE_SERVICE_CLIENT_SECRET", "integration-commerce-secret")
                    .withEnv("CINX_LEARNING_SERVICE_CLIENT_SECRET", "integration-learning-secret")
                    .withEnv("CINX_NOTIFICATION_SERVICE_CLIENT_SECRET", "integration-notification-secret")
                    .withEnv("CINX_REPORTING_SERVICE_CLIENT_SECRET", "integration-reporting-secret")
                    .withEnv("GOOGLE_CLIENT_ID", "integration-google-client")
                    .withEnv("GOOGLE_CLIENT_SECRET", "integration-google-secret")
                    .withCopyFileToContainer(
                            MountableFile.forHostPath(Path.of("..", "keycloak", "realm", "cinx-realm.json")
                                    .toAbsolutePath().normalize()),
                            "/opt/keycloak/data/import/cinx-realm.json")
                    .withExposedPorts(8080)
                    .withCommand("start-dev", "--import-realm")
                    .waitingFor(org.testcontainers.containers.wait.strategy.Wait.forHttp("/realms/cinx")
                            .forPort(8080).forStatusCode(200));

    @Test
    void importsPkceBrokerUidRolesAndLeastPrivilegeServiceAccount() throws Exception {
        String base = "http://" + KEYCLOAK.getHost() + ":" + KEYCLOAK.getMappedPort(8080);
        JsonNode discovery = get(base + "/realms/cinx/.well-known/openid-configuration", null);
        assertThat(discovery.path("issuer").asText()).isEqualTo(base + "/realms/cinx");

        String token = clientCredentialsToken(base);
        assertThat(token).isNotBlank();
        JsonNode clients = get(base + "/admin/realms/cinx/clients?clientId=cinx-web", token);
        JsonNode web = clients.get(0);
        assertThat(web.path("publicClient").asBoolean()).isTrue();
        assertThat(web.path("directAccessGrantsEnabled").asBoolean()).isFalse();
        assertThat(web.path("attributes").path("pkce.code.challenge.method").asText()).isEqualTo("S256");

        JsonNode realm = get(base + "/admin/realms/cinx", token);
        assertThat(realm.path("accessTokenLifespan").asInt()).isEqualTo(300);
        assertThat(get(base + "/admin/realms/cinx/roles", token).toString())
                .contains("LEARNER", "INSTRUCTOR", "PLATFORM_ADMIN", "BREAK_GLASS_ADMIN");

        JsonNode mappers = get(base + "/admin/realms/cinx/clients/" + web.path("id").asText()
                + "/protocol-mappers/models", token);
        assertThat(mappers.toString()).contains(
                "\"user.attribute\":\"uid\"",
                "\"claim.name\":\"uid\"",
                "oidc-usermodel-attribute-mapper");

        JsonNode brokers = get(base + "/admin/realms/cinx/identity-provider/instances", token);
        assertThat(brokers.toString()).contains("\"alias\":\"google\"", "\"trustEmail\":false");

        String userLocation = post(base + "/admin/realms/cinx/users", token, """
                {"username":"imported@example.test","email":"imported@example.test","enabled":true,
                 "attributes":{"uid":["legacy-uid-1"]},"requiredActions":["UPDATE_PASSWORD"],
                 "credentials":[{"type":"password","value":"Temporary-123!","temporary":true}]}
                """);
        String userId = userLocation.substring(userLocation.lastIndexOf('/') + 1);
        JsonNode imported = get(base + "/admin/realms/cinx/users/" + userId, token);
        assertThat(imported.path("attributes").path("uid").get(0).asText()).isEqualTo("legacy-uid-1");
        assertThat(imported.path("requiredActions").toString()).contains("UPDATE_PASSWORD");

        JsonNode instructor = get(base + "/admin/realms/cinx/roles/INSTRUCTOR", token);
        post(base + "/admin/realms/cinx/users/" + userId + "/role-mappings/realm", token,
                "[" + instructor + "]");
        assertThat(get(base + "/admin/realms/cinx/users/" + userId + "/role-mappings/realm", token).toString())
                .contains("\"name\":\"INSTRUCTOR\"");
    }

    private static String clientCredentialsToken(String base) throws Exception {
        String body = form(Map.of(
                "grant_type", "client_credentials",
                "client_id", "cinx-auth-admin",
                "client_secret", ADMIN_SECRET));
        HttpRequest request = HttpRequest.newBuilder(URI.create(base + "/realms/cinx/protocol/openid-connect/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        return JSON.readTree(response.body()).path("access_token").asText();
    }

    private static JsonNode get(String url, String bearer) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(url)).GET();
        if (bearer != null) {
            request.header("Authorization", "Bearer " + bearer);
        }
        HttpResponse<String> response = HTTP.send(request.build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
        return JSON.readTree(response.body());
    }

    private static String post(String url, String bearer, String json) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("Authorization", "Bearer " + bearer)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).as(response.body()).isBetween(200, 204);
        return response.headers().firstValue("Location").orElse("");
    }

    private static String form(Map<String, String> values) {
        return values.entrySet().stream()
                .map(e -> java.net.URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "="
                        + java.net.URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
    }
}
