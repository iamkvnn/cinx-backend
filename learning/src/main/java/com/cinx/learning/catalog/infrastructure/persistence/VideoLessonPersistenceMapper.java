package com.cinx.learning.catalog.infrastructure.persistence;

import com.cinx.learning.catalog.domain.VideoLesson;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface VideoLessonPersistenceMapper {
    VideoLesson toDomain(VideoLessonJpaEntity entity);

    VideoLessonJpaEntity toEntity(VideoLesson domain);

    // the key, the lock version and the audit stamps belong to the stored row
    @Mapping(target = "lessonId", ignore = true)
    @Mapping(target = "questions", ignore = true)
    @Mapping(target = "subtitles", ignore = true)
    void update(@MappingTarget VideoLessonJpaEntity entity, VideoLesson domain);
}
