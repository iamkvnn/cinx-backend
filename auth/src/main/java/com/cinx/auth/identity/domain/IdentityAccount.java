package com.cinx.auth.identity.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * What auth knows about a user beyond Keycloak: status, ban, and when their authorization last changed. Roles live in
 * Keycloak. The row is created on the first change, so a user nobody has touched has none.
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
public class IdentityAccount {
    /** The Keycloak subject. */
    private final String userId;
    private IdentityStatus status;
    /** Tokens issued before this instant are stale; null when nothing has changed yet. */
    private LocalDateTime changedAt;
    private String banReason;
    private String banDetails;
    private LocalDateTime banExpiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    /** The persistence version (optimistic lock); null until the account is first stored. */
    private Long version;

    /** A user seen for the first time: active, never changed. */
    public static IdentityAccount create(String userId, LocalDateTime now) {
        return IdentityAccount.builder().userId(userId).status(IdentityStatus.ACTIVE)
                .createdAt(now).updatedAt(now).build();
    }

    /** Records an authorization change, so every token issued before {@code now} is refused. */
    public void markChanged(LocalDateTime now) {
        changedAt = now;
        updatedAt = now;
    }
}
