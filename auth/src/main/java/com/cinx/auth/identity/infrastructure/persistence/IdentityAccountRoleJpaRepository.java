package com.cinx.auth.identity.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

interface IdentityAccountRoleJpaRepository extends JpaRepository<IdentityAccountRoleJpaEntity, IdentityAccountRoleId> {
    List<IdentityAccountRoleJpaEntity> findAllByUserId(String userId);

    /** Every grant in force at {@code now}, across all accounts. */
    @Query("select r from IdentityAccountRoleJpaEntity r where r.status = com.cinx.auth.identity.domain.AuthorizationGrantStatus.ACTIVE and (r.expiresAt is null or r.expiresAt > :now)")
    List<IdentityAccountRoleJpaEntity> findAllEffective(LocalDateTime now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from IdentityAccountRoleJpaEntity r where r.status = com.cinx.auth.identity.domain.AuthorizationGrantStatus.ACTIVE and r.expiresAt is not null and r.expiresAt <= :now")
    List<IdentityAccountRoleJpaEntity> lockDue(LocalDateTime now);
}
