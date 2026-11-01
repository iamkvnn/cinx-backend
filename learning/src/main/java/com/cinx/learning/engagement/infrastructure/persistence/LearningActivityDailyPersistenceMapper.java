package com.cinx.learning.engagement.infrastructure.persistence;

import com.cinx.learning.engagement.domain.LearningActivityDaily;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface LearningActivityDailyPersistenceMapper {
    LearningActivityDaily toDomain(LearningActivityDailyJpaEntity entity);

    LearningActivityDailyJpaEntity toEntity(LearningActivityDaily domain);

    // the key, the lock version and the audit stamps belong to the stored row
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    void update(@MappingTarget LearningActivityDailyJpaEntity entity, LearningActivityDaily domain);
}
