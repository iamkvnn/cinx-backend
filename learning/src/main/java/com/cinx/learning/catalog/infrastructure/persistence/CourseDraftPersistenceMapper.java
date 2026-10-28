package com.cinx.learning.catalog.infrastructure.persistence;

import com.cinx.learning.catalog.domain.CourseDraft;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR, uses = {CatalogEntityRefs.class})
interface CourseDraftPersistenceMapper {
    @Mapping(target = "courseId", source = "course.id")
    @Mapping(target = "categoryId", source = "category.id")
    CourseDraft toDomain(CourseDraftJpaEntity entity);

    @Mapping(target = "course", source = "courseId")
    @Mapping(target = "category", source = "categoryId")
    CourseDraftJpaEntity toEntity(CourseDraft domain);

    // the key, the lock version and the audit stamps belong to the stored row
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "course", source = "courseId")
    @Mapping(target = "category", source = "categoryId")
    void update(@MappingTarget CourseDraftJpaEntity entity, CourseDraft domain);
}
