package com.cinx.auth.identity.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LinkIdentityRequest(
        @NotBlank String keycloakSubject,
        @Email String email
) {
}
