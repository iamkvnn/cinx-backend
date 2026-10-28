package com.cinx.learning.catalog.infrastructure.persistence;

import com.cinx.learning.catalog.domain.Section;
import org.mapstruct.AfterMapping;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR, uses = {LessonPersistenceMapper.class, CatalogEntityRefs.class})
interface SectionPersistenceMapper {
    @Mapping(target = "courseId", source = "course.id")
    @Mapping(target = "draftId", source = "draft.id")
    Section toDomain(SectionJpaEntity entity);

    @Mapping(target = "course", source = "courseId")
    @Mapping(target = "draft", source = "draftId")
    SectionJpaEntity toEntity(Section domain);

    // the key, the lock version and the audit stamps belong to the stored row
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "course", source = "courseId")
    @Mapping(target = "draft", source = "draftId")
    @Mapping(target = "lessons", ignore = true)
    void update(@MappingTarget SectionJpaEntity entity, Section domain);

    @AfterMapping
    default void linkLessons(@MappingTarget SectionJpaEntity entity) {
        entity.getLessons().forEach(child -> child.setSection(entity));
    }
}
