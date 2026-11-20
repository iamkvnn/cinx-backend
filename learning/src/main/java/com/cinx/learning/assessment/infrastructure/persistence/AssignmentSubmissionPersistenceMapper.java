package com.cinx.learning.assessment.infrastructure.persistence;

import com.cinx.learning.assessment.domain.AssignmentSubmission;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR, uses = {AssignmentSubmissionAttachmentPersistenceMapper.class})
interface AssignmentSubmissionPersistenceMapper {
    AssignmentSubmission toDomain(AssignmentSubmissionJpaEntity entity);

    @Mapping(target = "attachments", ignore = true)
    AssignmentSubmissionJpaEntity toEntity(AssignmentSubmission domain);

    // the key, the lock version and the audit stamps belong to the stored row
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "attachments", ignore = true)
    void update(@MappingTarget AssignmentSubmissionJpaEntity entity, AssignmentSubmission domain);
}
