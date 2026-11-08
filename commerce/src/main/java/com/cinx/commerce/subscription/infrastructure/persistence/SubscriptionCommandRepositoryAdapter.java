package com.cinx.commerce.subscription.infrastructure.persistence;

import com.cinx.commerce.subscription.domain.SubscriptionCommand;
import com.cinx.commerce.subscription.domain.SubscriptionCommandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
class SubscriptionCommandRepositoryAdapter implements SubscriptionCommandRepository {
    private final SubscriptionCommandJpaRepository jpa;
    private final SubscriptionCommandPersistenceMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public Optional<SubscriptionCommand> findBySubjectIdAndIdempotencyKey(String subjectId, String idempotencyKey) {
        return jpa.findBySubjectIdAndIdempotencyKey(subjectId, idempotencyKey).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public Optional<SubscriptionCommand> insertIfAbsent(SubscriptionCommand command) {
        try {
            return Optional.of(mapper.toDomain(jpa.saveAndFlush(mapper.toEntity(command))));
        } catch (DataIntegrityViolationException duplicate) {
            return Optional.empty();
        }
    }

    @Override
    @Transactional
    public SubscriptionCommand save(SubscriptionCommand command) {
        SubscriptionCommandJpaEntity stored = jpa.findById(command.getCommandId()).orElse(null);
        if (stored == null) {
            return mapper.toDomain(jpa.save(mapper.toEntity(command)));
        }
        mapper.update(stored, command);
        return mapper.toDomain(jpa.save(stored));
    }
}
