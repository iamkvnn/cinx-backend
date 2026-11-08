package com.cinx.commerce.voucher.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

interface VoucherJpaRepository extends JpaRepository<VoucherJpaEntity, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select distinct v from VoucherJpaEntity v left join fetch v.targetRules where v.code = :code")
    Optional<VoucherJpaEntity> findByCodeForUpdate(String code);

    @EntityGraph(attributePaths = "targetRules")
    Optional<VoucherJpaEntity> findDetailedByCode(String code);

    boolean existsByCode(String code);
}
