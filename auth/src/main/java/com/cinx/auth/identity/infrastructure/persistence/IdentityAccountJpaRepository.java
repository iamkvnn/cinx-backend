package com.cinx.auth.identity.infrastructure.persistence;

import com.cinx.auth.identity.domain.IdentityStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

interface IdentityAccountJpaRepository extends JpaRepository<IdentityAccountJpaEntity, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from IdentityAccountJpaEntity a where a.userId = :userId")
    Optional<IdentityAccountJpaEntity> lockByUserId(String userId);

    List<IdentityAccountJpaEntity> findAllByStatus(IdentityStatus status);
}
