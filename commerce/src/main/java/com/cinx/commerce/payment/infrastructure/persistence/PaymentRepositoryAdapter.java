package com.cinx.commerce.payment.infrastructure.persistence;

import com.cinx.commerce.payment.domain.Payment;
import com.cinx.commerce.payment.domain.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class PaymentRepositoryAdapter implements PaymentRepository {
    private final PaymentJpaRepository jpa;
    private final PaymentPersistenceMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public boolean existsByProviderAndProviderEventId(String provider, String providerEventId) {
        return jpa.existsByProviderAndProviderEventId(provider, providerEventId);
    }

    @Override
    @Transactional
    public Payment save(Payment payment) {
        return mapper.toDomain(jpa.save(mapper.toEntity(payment)));
    }
}
