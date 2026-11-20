package com.cinx.commerce.subscription.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface BillingWebhookInboxJpaRepository extends JpaRepository<BillingWebhookInboxJpaEntity, BillingWebhookInboxJpaEntity.Key> {
}
