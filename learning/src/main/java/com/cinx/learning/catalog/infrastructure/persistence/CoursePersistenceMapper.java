package com.cinx.learning.catalog.infrastructure.persistence;

import com.cinx.learning.catalog.domain.Course;
import org.mapstruct.AfterMapping;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR, uses = {CourseImagePersistenceMapper.class, CatalogEntityRefs.class})
interface CoursePersistenceMapper {
    @Mapping(target = "categoryId", source = "category.id")
    Course toDomain(CourseJpaEntity entity);

    @Mapping(target = "category", source = "categoryId")
    CourseJpaEntity toEntity(Course domain);

    // the key, the lock version and the audit stamps belong to the stored row
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "category", source = "categoryId")
    @Mapping(target = "images", ignore = true)
    void update(@MappingTarget CourseJpaEntity entity, Course domain);

    @AfterMapping
    default void linkImages(@MappingTarget CourseJpaEntity entity) {
        entity.getImages().forEach(child -> child.setCourse(entity));
    }
}
