package com.cinx.learning.assessment.infrastructure.persistence;

import com.cinx.learning.assessment.domain.LearningItemResult;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface LearningItemResultPersistenceMapper {
    LearningItemResult toDomain(LearningItemResultJpaEntity entity);

    LearningItemResultJpaEntity toEntity(LearningItemResult domain);

    // the key, the lock version and the audit stamps belong to the stored row
    @Mapping(target = "itemId", ignore = true)
    void update(@MappingTarget LearningItemResultJpaEntity entity, LearningItemResult domain);
}
