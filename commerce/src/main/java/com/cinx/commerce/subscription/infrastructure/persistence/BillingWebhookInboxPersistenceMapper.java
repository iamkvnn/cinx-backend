package com.cinx.commerce.subscription.infrastructure.persistence;

import com.cinx.commerce.subscription.domain.BillingWebhookInbox;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
interface BillingWebhookInboxPersistenceMapper {
    BillingWebhookInboxJpaEntity toEntity(BillingWebhookInbox domain);

    BillingWebhookInbox toDomain(BillingWebhookInboxJpaEntity entity);

    void update(@MappingTarget BillingWebhookInboxJpaEntity entity, BillingWebhookInbox domain);
}
