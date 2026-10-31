package com.cinx.learning.assessment.infrastructure.persistence;

import com.cinx.learning.assessment.domain.QuizSessionSubmission;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface QuizSessionSubmissionPersistenceMapper {
    QuizSessionSubmission toDomain(QuizSessionSubmissionJpaEntity entity);

    QuizSessionSubmissionJpaEntity toEntity(QuizSessionSubmission domain);

    // the key, the lock version and the audit stamps belong to the stored row
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "quizSession", ignore = true)
    void update(@MappingTarget QuizSessionSubmissionJpaEntity entity, QuizSessionSubmission domain);
}
