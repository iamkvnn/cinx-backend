package com.cinx.commerce.subscription.infrastructure.persistence;

import com.cinx.commerce.subscription.domain.CommerceSubscription;
import com.cinx.commerce.subscription.domain.CommerceSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class CommerceSubscriptionRepositoryAdapter implements CommerceSubscriptionRepository {
    private final CommerceSubscriptionJpaRepository jpa;
    private final CommerceSubscriptionPersistenceMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public Optional<CommerceSubscription> findById(String id) {
        return jpa.findById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommerceSubscription> findAllBySubjectId(String subjectId) {
        return jpa.findAllBySubjectIdOrderByCreatedAtDesc(subjectId).stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommerceSubscription> findReconcilable() {
        return jpa.findTop100ByProviderIsNotNullAndProviderSubscriptionRefIsNotNullOrderByUpdatedAtAsc().stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CommerceSubscription> findByProviderRef(String provider, String providerAccountKey, String providerSubscriptionRef) {
        return jpa.findByProviderAndProviderAccountKeyAndProviderSubscriptionRef(provider, providerAccountKey, providerSubscriptionRef)
                .map(mapper::toDomain);
    }

    /** Writes a new subscription out at once, or the changes onto the stored row so its lock version stays the store's. */
    @Override
    @Transactional
    public CommerceSubscription save(CommerceSubscription subscription) {
        CommerceSubscriptionJpaEntity stored = jpa.findById(subscription.getId()).orElse(null);
        if (stored == null) {
            return mapper.toDomain(jpa.saveAndFlush(mapper.toEntity(subscription)));
        }
        mapper.update(stored, subscription);
        return mapper.toDomain(jpa.save(stored));
    }
}
