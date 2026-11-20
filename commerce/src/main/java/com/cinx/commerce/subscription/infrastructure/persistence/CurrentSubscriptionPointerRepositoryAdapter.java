package com.cinx.commerce.subscription.infrastructure.persistence;

import com.cinx.commerce.subscription.domain.CurrentSubscriptionPointer;
import com.cinx.commerce.subscription.domain.CurrentSubscriptionPointerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
class CurrentSubscriptionPointerRepositoryAdapter implements CurrentSubscriptionPointerRepository {
    private final CurrentSubscriptionPointerJpaRepository jpa;
    private final CurrentSubscriptionPointerPersistenceMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public Optional<CurrentSubscriptionPointer> find(String subjectId, String slotCode) {
        return jpa.findById(new CurrentSubscriptionPointerJpaEntity.Key(subjectId, slotCode)).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public Optional<CurrentSubscriptionPointer> lock(String subjectId, String slotCode) {
        return jpa.lockBySubjectIdAndSubscriptionSlotCode(subjectId, slotCode).map(mapper::toDomain);
    }

    /** Inserts a new pointer, or writes the changes onto the stored row so its lock version stays the store's. */
    @Override
    @Transactional
    public CurrentSubscriptionPointer save(CurrentSubscriptionPointer pointer) {
        CurrentSubscriptionPointerJpaEntity stored = jpa.findById(
                new CurrentSubscriptionPointerJpaEntity.Key(pointer.getSubjectId(), pointer.getSubscriptionSlotCode())).orElse(null);
        if (stored == null) {
            return mapper.toDomain(jpa.save(mapper.toEntity(pointer)));
        }
        mapper.update(stored, pointer);
        return mapper.toDomain(jpa.save(stored));
    }
}
