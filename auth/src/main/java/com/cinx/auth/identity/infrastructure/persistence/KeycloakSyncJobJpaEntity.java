package com.cinx.auth.identity.infrastructure.persistence;

import com.cinx.auth.identity.domain.KeycloakSyncJob;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "keycloak_sync_job")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KeycloakSyncJobJpaEntity {
    @Id private String id;
    @Column(name = "user_id", nullable = false) private String userId;
    @Column(name = "desired_authorization_version", nullable = false) private long desiredAuthorizationVersion;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private KeycloakSyncJob.Status status;
    @Column(name = "attempt_count", nullable = false) private int attemptCount;
    @Column(name = "next_attempt_at", nullable = false) private LocalDateTime nextAttemptAt;
    @Column(name = "last_error_code") private String lastErrorCode;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;
}
