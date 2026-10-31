package com.cinx.learning.enrollment.infrastructure.persistence;

import com.cinx.learning.enrollment.domain.CourseEntitlement;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface CourseEntitlementPersistenceMapper {
    CourseEntitlement toDomain(CourseEntitlementJpaEntity entity);

    CourseEntitlementJpaEntity toEntity(CourseEntitlement domain);

    // the key, the lock version and the audit stamps belong to the stored row
    @Mapping(target = "id", ignore = true)
    void update(@MappingTarget CourseEntitlementJpaEntity entity, CourseEntitlement domain);
}
