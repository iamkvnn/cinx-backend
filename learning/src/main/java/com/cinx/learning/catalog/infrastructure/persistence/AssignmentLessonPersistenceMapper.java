package com.cinx.learning.catalog.infrastructure.persistence;

import com.cinx.learning.catalog.domain.AssignmentLesson;
import org.mapstruct.AfterMapping;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR, uses = {AssignmentAttachmentPersistenceMapper.class})
interface AssignmentLessonPersistenceMapper {
    AssignmentLesson toDomain(AssignmentLessonJpaEntity entity);

    AssignmentLessonJpaEntity toEntity(AssignmentLesson domain);

    // the key, the lock version and the audit stamps belong to the stored row
    @Mapping(target = "lessonId", ignore = true)
    @Mapping(target = "attachments", ignore = true)
    void update(@MappingTarget AssignmentLessonJpaEntity entity, AssignmentLesson domain);

    @AfterMapping
    default void linkAttachments(@MappingTarget AssignmentLessonJpaEntity entity) {
        entity.getAttachments().forEach(child -> child.setAssignmentLesson(entity));
    }
}
