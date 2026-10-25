package com.cinx.auth.identity.infrastructure.persistence;

import com.cinx.auth.identity.domain.IdentityStatus;
import com.cinx.auth.identity.domain.IdentitySyncStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "identity_account")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IdentityAccountJpaEntity {
    @Id
    @Column(name = "user_id", length = 255)
    private String userId;
    @Column(name = "keycloak_subject", unique = true, length = 255)
    private String keycloakSubject;
    @Column(length = 320)
    private String email;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private IdentityStatus status;
    @Enumerated(EnumType.STRING)
    @Column(name = "sync_status", nullable = false, length = 40)
    private IdentitySyncStatus syncStatus;
    @Column(name = "authorization_version", nullable = false)
    private Long authorizationVersion;
    @Column(name = "authorization_policy_version", nullable = false)
    private Long authorizationPolicyVersion;
    @Column(name = "ban_reason", length = 100)
    private String banReason;
    @Column(name = "ban_details", length = 2000)
    private String banDetails;
    @Column(name = "ban_expires_at")
    private LocalDateTime banExpiresAt;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    @Version
    private Long version;
}
