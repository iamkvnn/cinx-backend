package com.cinx.auth.identity.api.dto;

import com.cinx.auth.identity.domain.AuthorizationRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.time.Instant;
import java.util.Set;

public record UpdateRolesRequest(@NotEmpty Set<AuthorizationRole> roles,
                                 @NotBlank String reason,
                                 Instant expiresAt) {
}
