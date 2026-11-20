package com.cinx.commerce.subscription.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface CommerceSubscriptionJpaRepository extends JpaRepository<CommerceSubscriptionJpaEntity, String> {
    List<CommerceSubscriptionJpaEntity> findAllBySubjectIdOrderByCreatedAtDesc(String subjectId);

    List<CommerceSubscriptionJpaEntity> findTop100ByProviderIsNotNullAndProviderSubscriptionRefIsNotNullOrderByUpdatedAtAsc();

    Optional<CommerceSubscriptionJpaEntity> findByProviderAndProviderAccountKeyAndProviderSubscriptionRef(
            String provider, String providerAccountKey, String providerSubscriptionRef);
}
