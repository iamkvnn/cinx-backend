package com.cinx.learning.progress.infrastructure.persistence;

import com.cinx.learning.progress.domain.CourseProgress;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface CourseProgressPersistenceMapper {
    CourseProgress toDomain(CourseProgressJpaEntity entity);

    CourseProgressJpaEntity toEntity(CourseProgress domain);

    // the key, the lock version and the audit stamps belong to the stored row
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "learningItemProgressList", ignore = true)
    void update(@MappingTarget CourseProgressJpaEntity entity, CourseProgress domain);
}
