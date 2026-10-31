package com.cinx.auth.identity.domain;

import java.util.Optional;

/** Port: where identity accounts are stored. */
public interface IdentityAccountRepository {
    Optional<IdentityAccount> findById(String userId);

    /** Loads the account and holds a write lock on it until the surrounding transaction ends. */
    Optional<IdentityAccount> lockById(String userId);

    /** Stores the account and returns it as stored. */
    IdentityAccount save(IdentityAccount account);
}
