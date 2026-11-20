package com.cinx.commerce.payment.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface PaymentJpaRepository extends JpaRepository<PaymentJpaEntity, String> {
    boolean existsByProviderAndProviderEventId(String provider, String providerEventId);
}
