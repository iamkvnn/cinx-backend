package com.cinx.auth.identity.application;

import com.cinx.auth.identity.domain.AuthorizationRole;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class AuthorizationCatalogContractTest {
    @Test
    void catalogRolesMatchCodeAndRealmHasRequiredClaimsAndServiceClients() throws Exception {
        AuthorizationCatalog catalog = new AuthorizationCatalog();
        assertThat(catalog.roles()).containsExactlyInAnyOrder(AuthorizationRole.values());
        assertThat(catalog.policyVersion()).isPositive();
        assertThat(catalog.effectivePermissions(Set.of(AuthorizationRole.LEARNER)))
                .contains("user.profile.read.self", "commerce.order.create.self")
                .allMatch(permission -> !permission.startsWith("ROLE_") && !permission.startsWith("PERM_"));

        Path realmPath = Path.of("..", "keycloak", "realm", "cinx-realm.json")
                .toAbsolutePath().normalize();
        JsonNode realm = new ObjectMapper().readTree(realmPath.toFile());
        Set<String> realmRoles = stream(realm.path("roles").path("realm"), "name");
        assertThat(realmRoles).containsExactlyInAnyOrderElementsOf(
                Arrays.stream(AuthorizationRole.values()).map(Enum::name).collect(Collectors.toSet()));
        for (AuthorizationRole role : AuthorizationRole.values()) {
            assertThat(realmPermissions(realm.path("roles").path("realm"), role.name(), new HashSet<>()))
                    .as("realm permissions for %s", role)
                    .containsExactlyInAnyOrderElementsOf(catalog.effectivePermissions(Set.of(role)));
        }
        Set<String> clients = stream(realm.path("clients"), "clientId");
        assertThat(clients).contains("cinx-api", "cinx-auth-internal", "cinx-commerce-internal", "cinx-learning-internal", "cinx-gateway-service",
                "cinx-user-service", "cinx-commerce-service", "cinx-learning-service",
                "cinx-notification-service", "cinx-reporting-service", "cinx-recommendation-service",
                "cinx-assistant-service");
        assertThat(realm.toString()).contains("\"claim.name\":\"permissions\"", "\"claim.name\":\"token_kind\"");
        // Keycloak owns roles and the user id is the subject: no claim auth used to write into Keycloak remains
        assertThat(realm.toString()).doesNotContain("\"claim.name\":\"authz_ver\"",
                "\"claim.name\":\"authz_not_after\"", "\"claim.name\":\"uid\"");
        assertThat(realm.toString()).doesNotContain("INSTRUCTOR_APPLICANT", "\"name\":\"ADMIN\"");
    }

    @Test
    void additiveMigrationDoesNotAutoPromoteLegacyAdmin() throws Exception {
        String migration = Files.readString(Path.of("src", "main", "resources", "db", "migration",
                "V3__authorization_foundation.sql"));

        assertThat(migration).contains("CREATE TABLE identity_account_role", "authorization_version")
                .doesNotContain("THEN 'PLATFORM_ADMIN'", "DROP COLUMN role");
    }

    private Set<String> stream(JsonNode array, String field) {
        java.util.Set<String> values = new java.util.HashSet<>();
        array.forEach(node -> values.add(node.path(field).asText()));
        return values;
    }

    private Set<String> realmPermissions(JsonNode roles, String roleName, Set<String> visited) {
        if (!visited.add(roleName)) return Set.of();
        JsonNode definition = null;
        for (JsonNode candidate : roles) {
            if (roleName.equals(candidate.path("name").asText())) {
                definition = candidate;
                break;
            }
        }
        if (definition == null) return Set.of();
        Set<String> permissions = new HashSet<>();
        definition.path("composites").path("client").path("cinx-api")
                .forEach(value -> permissions.add(value.asText()));
        definition.path("composites").path("realm")
                .forEach(value -> permissions.addAll(realmPermissions(roles, value.asText(), visited)));
        return permissions;
    }
}
