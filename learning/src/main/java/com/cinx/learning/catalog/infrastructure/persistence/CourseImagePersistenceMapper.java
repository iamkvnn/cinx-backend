package com.cinx.learning.catalog.infrastructure.persistence;

import com.cinx.learning.catalog.domain.CourseImage;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR, uses = {CatalogEntityRefs.class})
interface CourseImagePersistenceMapper {
    @Mapping(target = "courseId", source = "course.id")
    CourseImage toDomain(CourseImageJpaEntity entity);

    @Mapping(target = "course", source = "courseId")
    CourseImageJpaEntity toEntity(CourseImage domain);

    // the key, the lock version and the audit stamps belong to the stored row
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "course", source = "courseId")
    void update(@MappingTarget CourseImageJpaEntity entity, CourseImage domain);
}
