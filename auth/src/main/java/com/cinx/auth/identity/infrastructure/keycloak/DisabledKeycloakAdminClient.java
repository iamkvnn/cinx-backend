package com.cinx.auth.identity.infrastructure.keycloak;

import com.cinx.auth.identity.application.KeycloakAdminClient;

import com.cinx.auth.identity.domain.AuthorizationRole;
import java.time.Instant;
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
    public void synchronizeAuthorization(String keycloakSubject, String userId, boolean enabled,
                                         Set<AuthorizationRole> roles, long authorizationVersion,
                                         long policyVersion, Instant authorizationNotAfter) {
        throw disabled();
    }

    @Override
    public void revokeSessions(String keycloakSubject) {
        throw disabled();
    }
}
