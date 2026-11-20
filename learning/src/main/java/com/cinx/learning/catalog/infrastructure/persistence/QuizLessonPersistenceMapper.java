package com.cinx.learning.catalog.infrastructure.persistence;

import com.cinx.learning.catalog.domain.QuizLesson;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface QuizLessonPersistenceMapper {
    QuizLesson toDomain(QuizLessonJpaEntity entity);

    QuizLessonJpaEntity toEntity(QuizLesson domain);

    // the key, the lock version and the audit stamps belong to the stored row
    @Mapping(target = "lessonId", ignore = true)
    @Mapping(target = "questions", ignore = true)
    void update(@MappingTarget QuizLessonJpaEntity entity, QuizLesson domain);
}
