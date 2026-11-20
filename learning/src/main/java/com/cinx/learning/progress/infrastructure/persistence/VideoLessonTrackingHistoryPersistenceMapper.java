package com.cinx.learning.progress.infrastructure.persistence;

import com.cinx.learning.progress.domain.VideoLessonTrackingHistory;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface VideoLessonTrackingHistoryPersistenceMapper {
    VideoLessonTrackingHistory toDomain(VideoLessonTrackingHistoryJpaEntity entity);

    VideoLessonTrackingHistoryJpaEntity toEntity(VideoLessonTrackingHistory domain);

    // the key, the lock version and the audit stamps belong to the stored row
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    void update(@MappingTarget VideoLessonTrackingHistoryJpaEntity entity, VideoLessonTrackingHistory domain);
}
