package com.cinx.auth.identity.domain;

import java.time.LocalDateTime;
import java.util.List;

/** Port: where Keycloak sync jobs are stored. */
public interface KeycloakSyncJobRepository {
    boolean existsByUserIdAndDesiredAuthorizationVersion(String userId, long version);

    KeycloakSyncJob save(KeycloakSyncJob job);

    /** The pending or retryable jobs whose time has come, oldest first, locked until the transaction ends. */
    List<KeycloakSyncJob> lockDue(LocalDateTime now);
}
