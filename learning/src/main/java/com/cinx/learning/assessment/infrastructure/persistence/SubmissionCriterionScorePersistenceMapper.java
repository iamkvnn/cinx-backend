package com.cinx.learning.assessment.infrastructure.persistence;

import com.cinx.learning.assessment.domain.SubmissionCriterionScore;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface SubmissionCriterionScorePersistenceMapper {
    SubmissionCriterionScore toDomain(SubmissionCriterionScoreJpaEntity entity);

    SubmissionCriterionScoreJpaEntity toEntity(SubmissionCriterionScore domain);

    // the key, the lock version and the audit stamps belong to the stored row
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    void update(@MappingTarget SubmissionCriterionScoreJpaEntity entity, SubmissionCriterionScore domain);
}
