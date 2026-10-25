package com.cinx.auth.identity.application.query;

import com.cinx.auth.identity.domain.AuthorizationRole;

import java.util.Set;

/** An active account with the roles it holds right now. */
public record AccountRoles(String userId, Set<AuthorizationRole> roles) {
}
