package com.cinx.commerce.payment.infrastructure.persistence;

import com.cinx.commerce.payment.domain.PaymentAttempt;
import com.cinx.commerce.payment.domain.PaymentAttemptRepository;
import com.cinx.commerce.payment.domain.PaymentAttemptStatus;
import com.cinx.commerce.payment.domain.PaymentProviderName;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class PaymentAttemptRepositoryAdapter implements PaymentAttemptRepository {
    private final PaymentAttemptJpaRepository jpa;
    private final PaymentAttemptPersistenceMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public Optional<PaymentAttempt> findByProviderAndProviderReference(PaymentProviderName provider, String providerReference) {
        return jpa.findByProviderAndProviderReference(provider, providerReference).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PaymentAttempt> findLiveAttempt(String orderId, PaymentProviderName provider, LocalDateTime now) {
        return jpa.findFirstByOrderIdAndProviderAndStatusAndExpiresAtAfterOrderByCreatedAtDesc(
                orderId, provider, PaymentAttemptStatus.CREATED, now).map(mapper::toDomain);
    }

    /** Inserts a new attempt, or writes the changes onto the stored row. */
    @Override
    @Transactional
    public PaymentAttempt save(PaymentAttempt attempt) {
        PaymentAttemptJpaEntity stored = jpa.findById(attempt.getId()).orElse(null);
        if (stored == null) {
            return mapper.toDomain(jpa.save(mapper.toEntity(attempt)));
        }
        mapper.update(stored, attempt);
        return mapper.toDomain(jpa.save(stored));
    }
}
