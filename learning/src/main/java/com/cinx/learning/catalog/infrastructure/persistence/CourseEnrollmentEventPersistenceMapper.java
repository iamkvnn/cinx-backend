package com.cinx.learning.catalog.infrastructure.persistence;

import com.cinx.learning.catalog.domain.CourseEnrollmentEvent;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface CourseEnrollmentEventPersistenceMapper {
    CourseEnrollmentEvent toDomain(CourseEnrollmentEventJpaEntity entity);

    CourseEnrollmentEventJpaEntity toEntity(CourseEnrollmentEvent domain);

    // the key, the lock version and the audit stamps belong to the stored row
    @Mapping(target = "id", ignore = true)
    void update(@MappingTarget CourseEnrollmentEventJpaEntity entity, CourseEnrollmentEvent domain);
}
