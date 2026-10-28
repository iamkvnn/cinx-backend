package com.cinx.learning.catalog.infrastructure.persistence;

import com.cinx.learning.catalog.domain.VideoQuestion;
import org.mapstruct.AfterMapping;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR, uses = {VideoOptionPersistenceMapper.class, CatalogEntityRefs.class})
interface VideoQuestionPersistenceMapper {
    @Mapping(target = "videoLessonId", source = "videoLesson.lessonId")
    VideoQuestion toDomain(VideoQuestionJpaEntity entity);

    @Mapping(target = "videoLesson", source = "videoLessonId")
    VideoQuestionJpaEntity toEntity(VideoQuestion domain);

    // the key, the lock version and the audit stamps belong to the stored row
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "options", ignore = true)
    @Mapping(target = "videoLesson", source = "videoLessonId")
    void update(@MappingTarget VideoQuestionJpaEntity entity, VideoQuestion domain);

    @AfterMapping
    default void linkOptions(@MappingTarget VideoQuestionJpaEntity entity) {
        entity.getOptions().forEach(child -> child.setVideoQuestion(entity));
    }
}
