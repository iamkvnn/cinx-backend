package com.cinx.auth.identity.infrastructure.keycloak;

import com.cinx.auth.identity.application.KeycloakAdminClient;

import com.cinx.auth.identity.domain.AuthorizationRole;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "security.keycloak.enabled", havingValue = "false", matchIfMissing = true)
public class DisabledKeycloakAdminClient implements KeycloakAdminClient {
    private IllegalStateException disabled() {
        return new IllegalStateException("Keycloak integration is disabled");
    }

    @Override
    public Optional<KeycloakUser> findUser(String subject) {
        throw disabled();
    }

    @Override
    public Set<AuthorizationRole> directRoles(String subject) {
        throw disabled();
    }

    @Override
    public Set<AuthorizationRole> effectiveRoles(String subject) {
        throw disabled();
    }

    @Override
    public void addRoles(String subject, Set<AuthorizationRole> roles) {
        throw disabled();
    }

    @Override
    public void removeRoles(String subject, Set<AuthorizationRole> roles) {
        throw disabled();
    }

    @Override
    public void setEnabled(String subject, boolean enabled) {
        throw disabled();
    }

    @Override
    public List<String> enabledUsersWithRole(AuthorizationRole role) {
        throw disabled();
    }
}
