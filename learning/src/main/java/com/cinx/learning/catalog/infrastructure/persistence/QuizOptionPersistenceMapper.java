package com.cinx.learning.catalog.infrastructure.persistence;

import com.cinx.learning.catalog.domain.QuizOption;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR, uses = {CatalogEntityRefs.class})
interface QuizOptionPersistenceMapper {
    @Mapping(target = "quizQuestionId", source = "quizQuestion.id")
    QuizOption toDomain(QuizOptionJpaEntity entity);

    @Mapping(target = "quizQuestion", source = "quizQuestionId")
    QuizOptionJpaEntity toEntity(QuizOption domain);

    // the key, the lock version and the audit stamps belong to the stored row
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "quizQuestion", source = "quizQuestionId")
    void update(@MappingTarget QuizOptionJpaEntity entity, QuizOption domain);
}
