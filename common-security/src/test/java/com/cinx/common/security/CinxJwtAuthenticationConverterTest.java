package com.cinx.common.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CinxJwtAuthenticationConverterTest {
    private final CinxJwtAuthenticationConverter converter = new CinxJwtAuthenticationConverter();

    @Test
    void fallsBackToSubjectButDoesNotTreatSingleScopeAsLegacyRole() {
        Jwt jwt = jwt(Map.of("sub", "keycloak-subject", "scope", "INSTRUCTOR"));

        var authentication = converter.convert(jwt);

        assertThat(authentication.getName()).isEqualTo("keycloak-subject");
        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .contains("SCOPE_INSTRUCTOR")
                .doesNotContain("ROLE_INSTRUCTOR");
    }

    @Test
    void usesTheSubjectEvenWhenALegacyUidIsPresentAndConvertsKeycloakRolesAndScopes() {
        Jwt jwt = jwt(Map.of(
                "sub", "keycloak-subject",
                "uid", "cinx-user-id",
                "scope", "openid courses:write",
                "realm_access", Map.of("roles", List.of("INSTRUCTOR", "offline_access"))
        ));

        var authentication = converter.convert(jwt);

        assertThat(authentication.getName()).isEqualTo("keycloak-subject");
        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .contains("ROLE_INSTRUCTOR", "SCOPE_openid", "SCOPE_courses:write")
                .doesNotContain("ROLE_offline_access");
    }

    @Test
    void doesNotDoublePrefixAlreadyNormalizedRoles() {
        Jwt jwt = jwt(Map.of("sub", "keycloak-subject", "roles", List.of("ROLE_PLATFORM_ADMIN")));

        assertThat(converter.convert(jwt).getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_PLATFORM_ADMIN");
    }

    @Test
    void mapsValidDirectPermissionsAndDeduplicatesClaims() {
        Jwt jwt = jwt(Map.of(
                "sub", "subject",
                "permissions", List.of("learning.course.create", " ", "ROLE_ADMIN", "learning.course.create"),
                "plan", "PRO"));

        assertThat(converter.convert(jwt).getAuthorities())
                .extracting("authority")
                .containsExactly("PERM_learning.course.create");
    }

    @Test
    void fallsBackOnlyToExpectedResourceClientPermissions() {
        Jwt jwt = jwt(Map.of(
                "sub", "subject",
                "resource_access", Map.of(
                        "cinx-api", Map.of("roles", List.of("user.profile.read.self")),
                        "realm-management", Map.of("roles", List.of("manage-users")))));

        assertThat(converter.convert(jwt).getAuthorities())
                .extracting("authority")
                .containsExactly("PERM_user.profile.read.self");
    }

    @Test
    void supportsExactInternalResourceWithoutAddingHumanRoles() {
        var internal = new CinxJwtAuthenticationConverter("cinx-auth-internal", Set.of());
        Jwt jwt = jwt(Map.of(
                "sub", "service-account-cinx-notification-service",
                "permissions", List.of("auth.internal.permission-members.read"),
                "roles", List.of("PLATFORM_ADMIN")));

        assertThat(internal.convert(jwt).getAuthorities())
                .extracting("authority")
                .containsExactly("PERM_auth.internal.permission-members.read");
    }

    private Jwt jwt(Map<String, Object> claims) {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .claims(values -> values.putAll(claims))
                .build();
    }
}
