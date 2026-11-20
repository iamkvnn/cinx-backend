package com.cinx.auth.identity.application.event;

import com.cinx.auth.identity.domain.IdentityStatus;

import java.time.Instant;

/** In-process: an account's status or roles changed. Listeners that act outside the database wait for the commit. */
public record AuthorizationChanged(String userId, IdentityStatus status, Instant changedAt) {
}
