package com.cinx.commerce.catalog.infrastructure.persistence;

import com.cinx.commerce.catalog.domain.SellableCourse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
interface SellableCoursePersistenceMapper {
    SellableCourseJpaEntity toEntity(SellableCourse domain);

    SellableCourse toDomain(SellableCourseJpaEntity entity);

    void update(@MappingTarget SellableCourseJpaEntity entity, SellableCourse domain);
}
