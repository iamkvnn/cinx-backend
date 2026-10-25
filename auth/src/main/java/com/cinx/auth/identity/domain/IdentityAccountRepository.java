package com.cinx.auth.identity.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Port: where identity accounts, with their role grants, are stored. */
public interface IdentityAccountRepository {
    Optional<IdentityAccount> findById(String userId);

    /** Loads the account and holds a write lock on it until the surrounding transaction ends. */
    Optional<IdentityAccount> lockById(String userId);

    /** Stores the account and every one of its grants, and returns it as stored. */
    IdentityAccount save(IdentityAccount account);

    /** The accounts, locked, that hold an active grant whose end has passed. */
    List<IdentityAccount> lockAccountsWithDueGrants(LocalDateTime now);
}
