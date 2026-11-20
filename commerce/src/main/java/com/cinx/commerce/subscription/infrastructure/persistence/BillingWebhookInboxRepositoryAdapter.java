package com.cinx.commerce.subscription.infrastructure.persistence;

import com.cinx.commerce.subscription.domain.BillingWebhookInbox;
import com.cinx.commerce.subscription.domain.BillingWebhookInboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class BillingWebhookInboxRepositoryAdapter implements BillingWebhookInboxRepository {
    private final BillingWebhookInboxJpaRepository jpa;
    private final BillingWebhookInboxPersistenceMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public boolean exists(String provider, String providerAccountKey, String providerEventId) {
        return jpa.existsById(new BillingWebhookInboxJpaEntity.Key(provider, providerAccountKey, providerEventId));
    }

    @Override
    @Transactional
    public boolean insertIfAbsent(BillingWebhookInbox record) {
        try {
            jpa.saveAndFlush(mapper.toEntity(record));
            return true;
        } catch (DataIntegrityViolationException duplicate) {
            return false;
        }
    }

    @Override
    @Transactional
    public BillingWebhookInbox save(BillingWebhookInbox record) {
        BillingWebhookInboxJpaEntity stored = jpa.findById(
                new BillingWebhookInboxJpaEntity.Key(record.getProvider(), record.getProviderAccountKey(), record.getProviderEventId()))
                .orElse(null);
        if (stored == null) {
            return mapper.toDomain(jpa.save(mapper.toEntity(record)));
        }
        mapper.update(stored, record);
        return mapper.toDomain(jpa.save(stored));
    }
}
