package com.cinx.auth.identity.application.command;

import com.cinx.auth.identity.domain.AuthorizationRole;

import java.util.Set;

/** The staff roles an account should hold from now on. */
public record UpdateStaffRolesCommand(Set<AuthorizationRole> roles) {
}
