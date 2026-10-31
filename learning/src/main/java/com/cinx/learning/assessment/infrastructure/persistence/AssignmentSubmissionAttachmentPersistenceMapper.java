package com.cinx.learning.assessment.infrastructure.persistence;

import com.cinx.learning.assessment.domain.AssignmentSubmissionAttachment;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR, uses = {AssessmentEntityRefs.class})
interface AssignmentSubmissionAttachmentPersistenceMapper {
    @Mapping(target = "assignmentSubmissionId", source = "assignmentSubmission.id")
    AssignmentSubmissionAttachment toDomain(AssignmentSubmissionAttachmentJpaEntity entity);

    @Mapping(target = "assignmentSubmission", source = "assignmentSubmissionId")
    AssignmentSubmissionAttachmentJpaEntity toEntity(AssignmentSubmissionAttachment domain);

    // the key, the lock version and the audit stamps belong to the stored row
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "assignmentSubmission", source = "assignmentSubmissionId")
    void update(@MappingTarget AssignmentSubmissionAttachmentJpaEntity entity, AssignmentSubmissionAttachment domain);
}
