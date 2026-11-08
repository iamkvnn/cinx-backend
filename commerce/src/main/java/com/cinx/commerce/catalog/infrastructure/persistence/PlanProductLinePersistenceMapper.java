package com.cinx.commerce.catalog.infrastructure.persistence;

import com.cinx.commerce.catalog.domain.PlanProductLine;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
interface PlanProductLinePersistenceMapper {
    PlanProductLineJpaEntity toEntity(PlanProductLine domain);

    PlanProductLine toDomain(PlanProductLineJpaEntity entity);

    void update(@MappingTarget PlanProductLineJpaEntity entity, PlanProductLine domain);
}
