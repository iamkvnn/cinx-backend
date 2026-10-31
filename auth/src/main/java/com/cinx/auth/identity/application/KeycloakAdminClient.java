package com.cinx.auth.identity.application;

import com.cinx.auth.identity.domain.AuthorizationRole;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Port: Keycloak's admin API, the owner of users and their realm roles. Every call is synchronous. */
public interface KeycloakAdminClient {
    Optional<KeycloakUser> findUser(String subject);

    /** The CINX roles mapped to the user directly. */
    Set<AuthorizationRole> directRoles(String subject);

    /** The CINX roles the user holds, directly or through a composite such as the realm's default roles. */
    Set<AuthorizationRole> effectiveRoles(String subject);

    void addRoles(String subject, Set<AuthorizationRole> roles);

    void removeRoles(String subject, Set<AuthorizationRole> roles);

    /** A disabled user can neither sign in nor refresh a token. */
    void setEnabled(String subject, boolean enabled);

    /** The enabled users the role is mapped to directly. */
    List<String> enabledUsersWithRole(AuthorizationRole role);

    record KeycloakUser(String subject, String email, boolean enabled) {
    }
}
