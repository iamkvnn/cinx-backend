package com.cinx.auth.identity.application;

import com.cinx.auth.identity.domain.AuthorizationRole;
import com.cinx.auth.identity.domain.IdentityAccount;

import java.util.Set;

/** Port: tells the rest of the platform that an account's authorization changed. Delivery is the outbox's job. */
public interface IdentityEvents {
    void authorizationChanged(IdentityAccount account, Set<AuthorizationRole> effectiveRoles);
}
