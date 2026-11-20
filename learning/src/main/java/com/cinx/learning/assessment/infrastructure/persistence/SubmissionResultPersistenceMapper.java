package com.cinx.learning.assessment.infrastructure.persistence;

import com.cinx.learning.assessment.domain.SubmissionResult;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface SubmissionResultPersistenceMapper {
    SubmissionResult toDomain(SubmissionResultJpaEntity entity);

    SubmissionResultJpaEntity toEntity(SubmissionResult domain);

    // the key, the lock version and the audit stamps belong to the stored row
    @Mapping(target = "submissionId", ignore = true)
    void update(@MappingTarget SubmissionResultJpaEntity entity, SubmissionResult domain);
}
