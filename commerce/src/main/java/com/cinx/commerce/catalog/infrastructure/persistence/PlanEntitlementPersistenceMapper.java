package com.cinx.commerce.catalog.infrastructure.persistence;

import com.cinx.commerce.catalog.domain.PlanEntitlement;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
interface PlanEntitlementPersistenceMapper {
    PlanEntitlementJpaEntity toEntity(PlanEntitlement domain);

    PlanEntitlement toDomain(PlanEntitlementJpaEntity entity);

    void update(@MappingTarget PlanEntitlementJpaEntity entity, PlanEntitlement domain);
}
