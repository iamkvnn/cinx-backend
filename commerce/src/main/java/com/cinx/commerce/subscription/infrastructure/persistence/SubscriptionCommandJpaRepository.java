package com.cinx.commerce.subscription.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface SubscriptionCommandJpaRepository extends JpaRepository<SubscriptionCommandJpaEntity, String> {
    Optional<SubscriptionCommandJpaEntity> findBySubjectIdAndIdempotencyKey(String subjectId, String idempotencyKey);
}
