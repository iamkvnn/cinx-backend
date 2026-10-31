package com.cinx.learning.assessment.infrastructure.persistence;

import com.cinx.learning.assessment.domain.QuizSession;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface QuizSessionPersistenceMapper {
    QuizSession toDomain(QuizSessionJpaEntity entity);

    QuizSessionJpaEntity toEntity(QuizSession domain);

    // the key, the lock version and the audit stamps belong to the stored row
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "questions", ignore = true)
    @Mapping(target = "quizSessionSubmission", ignore = true)
    void update(@MappingTarget QuizSessionJpaEntity entity, QuizSession domain);
}
