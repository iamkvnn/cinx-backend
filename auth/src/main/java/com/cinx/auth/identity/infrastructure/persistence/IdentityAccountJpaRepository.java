package com.cinx.auth.identity.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

interface IdentityAccountJpaRepository extends JpaRepository<IdentityAccountJpaEntity, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from IdentityAccountJpaEntity a where a.userId = :userId")
    Optional<IdentityAccountJpaEntity> lockByUserId(String userId);
}
