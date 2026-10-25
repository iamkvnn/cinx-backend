package com.cinx.auth.identity.application;

import com.cinx.auth.identity.domain.AuthorizationRole;

import java.time.Instant;
import java.util.Set;

public interface KeycloakAdminClient {
    void synchronizeAuthorization(String keycloakSubject,
                                  String userId,
                                  boolean enabled,
                                  Set<AuthorizationRole> roles,
                                  long authorizationVersion,
                                  long policyVersion,
                                  Instant authorizationNotAfter);

    void revokeSessions(String keycloakSubject);
}
