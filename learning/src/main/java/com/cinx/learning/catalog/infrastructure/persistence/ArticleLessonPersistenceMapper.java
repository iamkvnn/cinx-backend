package com.cinx.learning.catalog.infrastructure.persistence;

import com.cinx.learning.catalog.domain.ArticleLesson;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface ArticleLessonPersistenceMapper {
    ArticleLesson toDomain(ArticleLessonJpaEntity entity);

    ArticleLessonJpaEntity toEntity(ArticleLesson domain);

    // the key, the lock version and the audit stamps belong to the stored row
    @Mapping(target = "lessonId", ignore = true)
    void update(@MappingTarget ArticleLessonJpaEntity entity, ArticleLesson domain);
}
