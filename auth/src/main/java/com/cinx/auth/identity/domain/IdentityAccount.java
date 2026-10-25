package com.cinx.auth.identity.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * A user's identity and what they may do: status, the roles they hold and the version that every other service
 * and Keycloak compare against. The role grants are part of the account, so they load and save with it.
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
public class IdentityAccount {
    private final String userId;
    private String keycloakSubject;
    private String email;
    private IdentityStatus status;
    private IdentitySyncStatus syncStatus;
    private Long authorizationVersion;
    private Long authorizationPolicyVersion;
    private String banReason;
    private String banDetails;
    private LocalDateTime banExpiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    /** The persistence version (optimistic lock); null until the account is first stored. */
    private Long version;
    @Builder.Default
    private List<RoleGrant> grants = new ArrayList<>();

    /** A new account: active, waiting for its first Keycloak sync, at authorization version 1. */
    public static IdentityAccount onboard(String userId, long policyVersion, LocalDateTime now) {
        return IdentityAccount.builder().userId(userId).createdAt(now).updatedAt(now)
                .status(IdentityStatus.ACTIVE).syncStatus(IdentitySyncStatus.PENDING)
                .authorizationVersion(1L).authorizationPolicyVersion(policyVersion).build();
    }

    public void incrementAuthorizationVersion() {
        authorizationVersion = authorizationVersion + 1;
    }

    public Optional<RoleGrant> grantOf(AuthorizationRole role) {
        return grants.stream().filter(grant -> grant.getRoleCode() == role).findFirst();
    }

    /**
     * Gives the role, replacing any earlier grant of it. Nothing changes when the role is already active with the same end.
     *
     * @return whether the account's roles changed
     */
    public boolean grantRole(AuthorizationRole role, String grantedBy, String source, LocalDateTime expiresAt, LocalDateTime now) {
        Optional<RoleGrant> existing = grantOf(role);
        if (existing.isPresent() && existing.get().isActive() && Objects.equals(existing.get().getExpiresAt(), expiresAt)) {
            return false;
        }
        grants.removeIf(grant -> grant.getRoleCode() == role);
        grants.add(RoleGrant.builder().roleCode(role).grantedBy(grantedBy).grantedAt(now).expiresAt(expiresAt)
                .status(AuthorizationGrantStatus.ACTIVE).source(source).build());
        return true;
    }

    /** @return whether an active grant of the role was revoked */
    public boolean revokeRole(AuthorizationRole role, LocalDateTime now) {
        Optional<RoleGrant> grant = grantOf(role).filter(RoleGrant::isActive);
        grant.ifPresent(found -> {
            found.setStatus(AuthorizationGrantStatus.REVOKED);
            found.setRevokedAt(now);
        });
        return grant.isPresent();
    }

    /** Marks every active grant that has ended as expired and returns them. */
    public List<RoleGrant> expireDueGrants(LocalDateTime now) {
        List<RoleGrant> due = grants.stream().filter(grant -> grant.isDueAt(now)).toList();
        due.forEach(grant -> {
            grant.setStatus(AuthorizationGrantStatus.EXPIRED);
            grant.setRevokedAt(now);
        });
        return due;
    }

    public List<RoleGrant> effectiveGrants(LocalDateTime now) {
        return grants.stream().filter(grant -> grant.isEffectiveAt(now)).toList();
    }

    public Set<AuthorizationRole> effectiveRoles(LocalDateTime now) {
        Set<AuthorizationRole> roles = EnumSet.noneOf(AuthorizationRole.class);
        effectiveGrants(now).forEach(grant -> roles.add(grant.getRoleCode()));
        return roles;
    }
}
