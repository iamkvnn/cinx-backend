package com.cinx.commerce.catalog.infrastructure.persistence;

import com.cinx.commerce.catalog.domain.PlanVersionTranslation;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
interface PlanVersionTranslationPersistenceMapper {
    PlanVersionTranslation toDomain(PlanVersionTranslationJpaEntity entity);
}
