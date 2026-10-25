package com.cinx.auth.identity.application.command;

/** Ties a user to their Keycloak subject; {@code email} is optional. */
public record LinkIdentityCommand(String keycloakSubject, String email) {
}
