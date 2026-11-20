package com.cinx.commerce.subscription.infrastructure.persistence;

import com.cinx.commerce.subscription.domain.SubscriptionAdjustment;
import com.cinx.commerce.subscription.domain.SubscriptionAdjustmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class SubscriptionAdjustmentRepositoryAdapter implements SubscriptionAdjustmentRepository {
    private final SubscriptionAdjustmentJpaRepository jpa;
    private final SubscriptionAdjustmentPersistenceMapper mapper;

    @Override
    @Transactional
    public SubscriptionAdjustment save(SubscriptionAdjustment adjustment) {
        return mapper.toDomain(jpa.save(mapper.toEntity(adjustment)));
    }
}
