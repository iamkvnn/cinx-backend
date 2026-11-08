package com.cinx.commerce.subscription.infrastructure.persistence;

import com.cinx.commerce.subscription.domain.SubscriptionSubject;
import com.cinx.commerce.subscription.domain.SubscriptionSubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
class SubscriptionSubjectRepositoryAdapter implements SubscriptionSubjectRepository {
    private final SubscriptionSubjectJpaRepository jpa;
    private final SubscriptionSubjectPersistenceMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public Optional<SubscriptionSubject> findById(String id) {
        return jpa.findById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SubscriptionSubject> findByTypeAndReference(String subjectType, String subjectReference) {
        return jpa.findBySubjectTypeAndSubjectReference(subjectType, subjectReference).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public Optional<SubscriptionSubject> lockByTypeAndReference(String subjectType, String subjectReference) {
        return jpa.lockBySubjectTypeAndSubjectReference(subjectType, subjectReference).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public SubscriptionSubject findOrCreate(SubscriptionSubject candidate) {
        return jpa.findBySubjectTypeAndSubjectReference(candidate.getSubjectType(), candidate.getSubjectReference())
                .map(mapper::toDomain).orElseGet(() -> {
                    try {
                        return mapper.toDomain(jpa.saveAndFlush(mapper.toEntity(candidate)));
                    } catch (DataIntegrityViolationException duplicate) {
                        return jpa.findBySubjectTypeAndSubjectReference(candidate.getSubjectType(), candidate.getSubjectReference())
                                .map(mapper::toDomain).orElseThrow();
                    }
                });
    }

    @Override
    @Transactional
    public SubscriptionSubject lockOrCreate(SubscriptionSubject candidate) {
        return jpa.lockBySubjectTypeAndSubjectReference(candidate.getSubjectType(), candidate.getSubjectReference())
                .map(mapper::toDomain).orElseGet(() -> {
                    try {
                        return mapper.toDomain(jpa.saveAndFlush(mapper.toEntity(candidate)));
                    } catch (DataIntegrityViolationException duplicate) {
                        return jpa.lockBySubjectTypeAndSubjectReference(candidate.getSubjectType(), candidate.getSubjectReference())
                                .map(mapper::toDomain).orElseThrow();
                    }
                });
    }
}
