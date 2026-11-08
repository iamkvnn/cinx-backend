package com.cinx.commerce.catalog.infrastructure.persistence;

import com.cinx.commerce.catalog.domain.SubscriptionPlanVersion;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
interface SubscriptionPlanVersionPersistenceMapper {
    SubscriptionPlanVersionJpaEntity toEntity(SubscriptionPlanVersion domain);

    SubscriptionPlanVersion toDomain(SubscriptionPlanVersionJpaEntity entity);

    // the lock version belongs to the stored row
    @Mapping(target = "optimisticVersion", ignore = true)
    void update(@MappingTarget SubscriptionPlanVersionJpaEntity entity, SubscriptionPlanVersion domain);
}
