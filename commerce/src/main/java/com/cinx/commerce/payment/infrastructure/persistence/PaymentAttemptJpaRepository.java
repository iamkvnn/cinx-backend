package com.cinx.commerce.payment.infrastructure.persistence;

import com.cinx.commerce.payment.domain.PaymentAttemptStatus;
import com.cinx.commerce.payment.domain.PaymentProviderName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

interface PaymentAttemptJpaRepository extends JpaRepository<PaymentAttemptJpaEntity, String> {
    Optional<PaymentAttemptJpaEntity> findByProviderAndProviderReference(PaymentProviderName provider, String providerReference);

    Optional<PaymentAttemptJpaEntity> findFirstByOrderIdAndProviderAndStatusAndExpiresAtAfterOrderByCreatedAtDesc(
            String orderId, PaymentProviderName provider, PaymentAttemptStatus status, LocalDateTime now);
}
