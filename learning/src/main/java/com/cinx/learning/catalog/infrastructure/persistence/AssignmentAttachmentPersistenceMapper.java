package com.cinx.learning.catalog.infrastructure.persistence;

import com.cinx.learning.catalog.domain.AssignmentAttachment;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR, uses = {CatalogEntityRefs.class})
interface AssignmentAttachmentPersistenceMapper {
    @Mapping(target = "assignmentLessonId", source = "assignmentLesson.lessonId")
    AssignmentAttachment toDomain(AssignmentAttachmentJpaEntity entity);

    @Mapping(target = "assignmentLesson", source = "assignmentLessonId")
    AssignmentAttachmentJpaEntity toEntity(AssignmentAttachment domain);

    // the key, the lock version and the audit stamps belong to the stored row
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "assignmentLesson", source = "assignmentLessonId")
    void update(@MappingTarget AssignmentAttachmentJpaEntity entity, AssignmentAttachment domain);
}
