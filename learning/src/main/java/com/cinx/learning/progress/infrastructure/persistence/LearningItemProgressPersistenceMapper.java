package com.cinx.learning.progress.infrastructure.persistence;

import com.cinx.learning.progress.domain.LearningItemProgress;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR, uses = {ProgressEntityRefs.class})
interface LearningItemProgressPersistenceMapper {
    @Mapping(target = "courseProgressId", source = "courseProgress.id")
    LearningItemProgress toDomain(LearningItemProgressJpaEntity entity);

    @Mapping(target = "courseProgress", source = "courseProgressId")
    LearningItemProgressJpaEntity toEntity(LearningItemProgress domain);

    // the key, the lock version and the audit stamps belong to the stored row
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "courseProgress", source = "courseProgressId")
    void update(@MappingTarget LearningItemProgressJpaEntity entity, LearningItemProgress domain);
}
