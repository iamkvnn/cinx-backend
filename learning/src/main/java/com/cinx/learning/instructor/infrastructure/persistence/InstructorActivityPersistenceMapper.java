package com.cinx.learning.instructor.infrastructure.persistence;

import com.cinx.learning.instructor.domain.InstructorActivity;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface InstructorActivityPersistenceMapper {
    InstructorActivity toDomain(InstructorActivityJpaEntity entity);

    InstructorActivityJpaEntity toEntity(InstructorActivity domain);

    // the key, the lock version and the audit stamps belong to the stored row
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    void update(@MappingTarget InstructorActivityJpaEntity entity, InstructorActivity domain);
}
