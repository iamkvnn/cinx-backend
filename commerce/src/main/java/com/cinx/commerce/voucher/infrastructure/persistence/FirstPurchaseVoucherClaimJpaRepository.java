package com.cinx.commerce.voucher.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface FirstPurchaseVoucherClaimJpaRepository extends JpaRepository<FirstPurchaseVoucherClaimJpaEntity, String> {
}
