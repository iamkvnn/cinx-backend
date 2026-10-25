package com.cinx.auth.identity.application.command;

import com.cinx.auth.identity.domain.AuthorizationRole;

import java.time.Instant;
import java.util.Set;

/** The staff roles an account should hold from now on, optionally ending at {@code expiresAt}. */
public record UpdateStaffRolesCommand(Set<AuthorizationRole> roles, Instant expiresAt) {
}
