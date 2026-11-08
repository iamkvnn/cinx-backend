package com.cinx.commerce.subscription.infrastructure.persistence;

import com.cinx.commerce.subscription.domain.SubscriptionAdjustment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
interface SubscriptionAdjustmentPersistenceMapper {
    SubscriptionAdjustmentJpaEntity toEntity(SubscriptionAdjustment domain);

    SubscriptionAdjustment toDomain(SubscriptionAdjustmentJpaEntity entity);

    void update(@MappingTarget SubscriptionAdjustmentJpaEntity entity, SubscriptionAdjustment domain);
}
