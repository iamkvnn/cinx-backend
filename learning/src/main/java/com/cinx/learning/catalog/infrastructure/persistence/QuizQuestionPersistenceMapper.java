package com.cinx.learning.catalog.infrastructure.persistence;

import com.cinx.learning.catalog.domain.QuizQuestion;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR, uses = {CatalogEntityRefs.class, QuizOptionPersistenceMapper.class})
interface QuizQuestionPersistenceMapper {
    @Mapping(target = "quizLessonId", source = "quizLesson.lessonId")
    QuizQuestion toDomain(QuizQuestionJpaEntity entity);

    @Mapping(target = "options", ignore = true)
    @Mapping(target = "quizLesson", source = "quizLessonId")
    QuizQuestionJpaEntity toEntity(QuizQuestion domain);

    // the key, the lock version and the audit stamps belong to the stored row
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "options", ignore = true)
    @Mapping(target = "quizLesson", source = "quizLessonId")
    void update(@MappingTarget QuizQuestionJpaEntity entity, QuizQuestion domain);
}
