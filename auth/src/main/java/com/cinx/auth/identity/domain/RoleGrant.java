package com.cinx.auth.identity.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** One role an account holds, with who gave it, why, and when it ends. It belongs to its {@link IdentityAccount}. */
@Getter
@Setter
@Builder
@AllArgsConstructor
public class RoleGrant {
    private final AuthorizationRole roleCode;
    private String grantedBy;
    private LocalDateTime grantedAt;
    private LocalDateTime expiresAt;
    private AuthorizationGrantStatus status;
    private LocalDateTime revokedAt;
    private String source;

    public boolean isActive() {
        return status == AuthorizationGrantStatus.ACTIVE;
    }

    /** In force right now: active, and either open-ended or ending after {@code now}. */
    public boolean isEffectiveAt(LocalDateTime now) {
        return isActive() && (expiresAt == null || expiresAt.isAfter(now));
    }

    /** Active and past its end. */
    public boolean isDueAt(LocalDateTime now) {
        return isActive() && expiresAt != null && !expiresAt.isAfter(now);
    }
}
