package com.cinx.learning.catalog.infrastructure.persistence;

import com.cinx.learning.catalog.domain.Lesson;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR, uses = {CatalogEntityRefs.class})
interface LessonPersistenceMapper {
    @Mapping(target = "sectionId", source = "section.id")
    Lesson toDomain(LessonJpaEntity entity);

    @Mapping(target = "section", source = "sectionId")
    LessonJpaEntity toEntity(Lesson domain);

    // the key, the lock version and the audit stamps belong to the stored row
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "section", source = "sectionId")
    void update(@MappingTarget LessonJpaEntity entity, Lesson domain);
}
