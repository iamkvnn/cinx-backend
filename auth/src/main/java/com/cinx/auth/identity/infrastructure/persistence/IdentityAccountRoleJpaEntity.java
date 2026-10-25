package com.cinx.auth.identity.infrastructure.persistence;

import com.cinx.auth.identity.domain.AuthorizationGrantStatus;
import com.cinx.auth.identity.domain.AuthorizationRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "identity_account_role")
@IdClass(IdentityAccountRoleId.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IdentityAccountRoleJpaEntity {
    @Id
    @Column(name = "user_id")
    private String userId;
    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "role_code")
    private AuthorizationRole roleCode;
    @Column(name = "granted_by", nullable = false)
    private String grantedBy;
    @Column(name = "granted_at", nullable = false)
    private LocalDateTime grantedAt;
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuthorizationGrantStatus status;
    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;
    @Column(nullable = false)
    private String source;
}
