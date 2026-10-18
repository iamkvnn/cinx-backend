package com.cinx.common.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Converts final Keycloak JWTs into CINX authorities.
 *
 * <p>Domain code uses the stable {@code uid} claim as the principal whenever it
 * is present. Keycloak realm roles become {@code ROLE_*} authorities and OAuth
 * scopes remain {@code SCOPE_*} authorities.</p>
 */
public class CinxJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {
    private static final Set<String> DEFAULT_CINX_ROLES = Set.of(
            "LEARNER", "INSTRUCTOR", "INSTRUCTOR_OPERATIONS", "CONTENT_REVIEWER",
            "CATALOG_MANAGER", "COMMUNITY_MODERATOR", "POLICY_MANAGER",
            "PROMOTION_MANAGER", "SUPPORT_AGENT", "IDENTITY_ADMIN",
            "FINANCE_OPERATOR", "REPORT_ANALYST", "NOTIFICATION_OPERATOR",
            "AI_OPERATOR", "PLATFORM_ADMIN", "BREAK_GLASS_ADMIN");
    private static final Pattern PERMISSION = Pattern.compile(
            "^[a-z][a-z0-9]*(?:[.-][a-z0-9]+)+$");

    private final String expectedResourceClient;
    private final Set<String> knownRoles;

    public CinxJwtAuthenticationConverter() {
        this("cinx-api", DEFAULT_CINX_ROLES);
    }

    public CinxJwtAuthenticationConverter(String expectedResourceClient) {
        this(expectedResourceClient, DEFAULT_CINX_ROLES);
    }

    public CinxJwtAuthenticationConverter(String expectedResourceClient, Set<String> knownRoles) {
        this.expectedResourceClient = expectedResourceClient;
        this.knownRoles = Set.copyOf(knownRoles);
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Set<GrantedAuthority> authorities = new LinkedHashSet<>();
        addDirectRoles(jwt, authorities);
        addKeycloakRealmRoles(jwt, authorities);
        addPermissions(jwt, authorities);
        addScopes(jwt, authorities);

        String principal = jwt.getClaimAsString("uid");
        if (principal == null || principal.isBlank()) {
            principal = jwt.getSubject();
        }
        return new JwtAuthenticationToken(jwt, authorities, principal);
    }

    private void addDirectRoles(Jwt jwt, Set<GrantedAuthority> authorities) {
        Object roles = jwt.getClaims().get("roles");
        if (roles instanceof Collection<?> values) {
            values.forEach(value -> addRole(String.valueOf(value), authorities));
        }
    }

    private void addKeycloakRealmRoles(Jwt jwt, Set<GrantedAuthority> authorities) {
        Object realmAccess = jwt.getClaims().get("realm_access");
        if (!(realmAccess instanceof Map<?, ?> access)) {
            return;
        }
        Object roles = access.get("roles");
        if (roles instanceof Collection<?> values) {
            values.forEach(value -> addRole(String.valueOf(value), authorities));
        }
    }

    private void addScopes(Jwt jwt, Set<GrantedAuthority> authorities) {
        String scope = jwt.getClaimAsString("scope");
        if (scope == null || scope.isBlank()) {
            return;
        }
        for (String value : scope.split(" ")) {
            if (!value.isBlank()) {
                authorities.add(new SimpleGrantedAuthority("SCOPE_" + value));
            }
        }
    }

    private void addPermissions(Jwt jwt, Set<GrantedAuthority> authorities) {
        Object permissions = jwt.getClaims().get("permissions");
        if (permissions instanceof Collection<?> values) {
            values.forEach(value -> addPermission(value, authorities));
            return;
        }
        if (expectedResourceClient == null || expectedResourceClient.isBlank()) {
            return;
        }
        Object resourceAccess = jwt.getClaims().get("resource_access");
        if (!(resourceAccess instanceof Map<?, ?> resources)) {
            return;
        }
        Object expected = resources.get(expectedResourceClient);
        if (expected instanceof Map<?, ?> access && access.get("roles") instanceof Collection<?> values) {
            values.forEach(value -> addPermission(value, authorities));
        }
    }

    private void addPermission(Object value, Set<GrantedAuthority> authorities) {
        if (!(value instanceof String permission)) {
            return;
        }
        String normalized = permission.trim();
        if (PERMISSION.matcher(normalized).matches()) {
            authorities.add(new SimpleGrantedAuthority("PERM_" + normalized));
        }
    }

    private void addRole(String role, Set<GrantedAuthority> authorities) {
        if (role == null || role.isBlank()) {
            return;
        }
        String normalized = role.startsWith("ROLE_") ? role.substring(5) : role;
        if (knownRoles.contains(normalized)) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + normalized));
        }
    }
}
