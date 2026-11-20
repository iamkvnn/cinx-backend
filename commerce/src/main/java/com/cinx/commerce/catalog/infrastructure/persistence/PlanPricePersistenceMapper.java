package com.cinx.commerce.catalog.infrastructure.persistence;

import com.cinx.commerce.catalog.domain.PlanPrice;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
interface PlanPricePersistenceMapper {
    PlanPriceJpaEntity toEntity(PlanPrice domain);

    PlanPrice toDomain(PlanPriceJpaEntity entity);

    void update(@MappingTarget PlanPriceJpaEntity entity, PlanPrice domain);
}
