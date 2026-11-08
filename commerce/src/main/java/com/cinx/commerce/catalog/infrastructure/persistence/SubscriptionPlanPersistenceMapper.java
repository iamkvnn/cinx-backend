package com.cinx.commerce.catalog.infrastructure.persistence;

import com.cinx.commerce.catalog.domain.SubscriptionPlan;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
interface SubscriptionPlanPersistenceMapper {
    SubscriptionPlanJpaEntity toEntity(SubscriptionPlan domain);

    SubscriptionPlan toDomain(SubscriptionPlanJpaEntity entity);

    void update(@MappingTarget SubscriptionPlanJpaEntity entity, SubscriptionPlan domain);
}
