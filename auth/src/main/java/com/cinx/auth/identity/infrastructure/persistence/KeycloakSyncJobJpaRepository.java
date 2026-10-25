package com.cinx.auth.identity.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

interface KeycloakSyncJobJpaRepository extends JpaRepository<KeycloakSyncJobJpaEntity, String> {
    boolean existsByUserIdAndDesiredAuthorizationVersion(String userId, long version);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select j from KeycloakSyncJobJpaEntity j where j.status in (com.cinx.auth.identity.domain.KeycloakSyncJob.Status.PENDING, com.cinx.auth.identity.domain.KeycloakSyncJob.Status.RETRYABLE_FAILED) and j.nextAttemptAt <= :now order by j.createdAt")
    List<KeycloakSyncJobJpaEntity> lockDue(LocalDateTime now);
}
