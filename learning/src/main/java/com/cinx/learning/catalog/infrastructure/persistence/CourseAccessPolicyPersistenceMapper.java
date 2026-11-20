package com.cinx.learning.catalog.infrastructure.persistence;

import com.cinx.learning.catalog.domain.CourseAccessPolicy;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface CourseAccessPolicyPersistenceMapper {
    CourseAccessPolicy toDomain(CourseAccessPolicyJpaEntity entity);

    CourseAccessPolicyJpaEntity toEntity(CourseAccessPolicy domain);

    // the key, the lock version and the audit stamps belong to the stored row
    @Mapping(target = "courseId", ignore = true)
    @Mapping(target = "version", ignore = true)
    void update(@MappingTarget CourseAccessPolicyJpaEntity entity, CourseAccessPolicy domain);
}
