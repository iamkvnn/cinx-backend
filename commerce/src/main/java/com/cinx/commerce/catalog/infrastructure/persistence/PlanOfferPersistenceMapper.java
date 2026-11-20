package com.cinx.commerce.catalog.infrastructure.persistence;

import com.cinx.commerce.catalog.domain.PlanOffer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
interface PlanOfferPersistenceMapper {
    PlanOfferJpaEntity toEntity(PlanOffer domain);

    PlanOffer toDomain(PlanOfferJpaEntity entity);

    void update(@MappingTarget PlanOfferJpaEntity entity, PlanOffer domain);
}
