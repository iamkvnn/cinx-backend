package com.cinx.commerce.voucher.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface VoucherRedemptionJpaRepository extends JpaRepository<VoucherRedemptionJpaEntity, String> {
    long countByVoucherIdAndUserId(String voucherId, String userId);

    boolean existsByVoucherId(String voucherId);
}
