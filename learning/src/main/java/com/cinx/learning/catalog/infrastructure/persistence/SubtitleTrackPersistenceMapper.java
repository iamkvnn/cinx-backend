package com.cinx.learning.catalog.infrastructure.persistence;

import com.cinx.learning.catalog.domain.SubtitleTrack;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR, uses = {CatalogEntityRefs.class})
interface SubtitleTrackPersistenceMapper {
    @Mapping(target = "videoLessonId", source = "videoLesson.lessonId")
    SubtitleTrack toDomain(SubtitleTrackJpaEntity entity);

    @Mapping(target = "videoLesson", source = "videoLessonId")
    SubtitleTrackJpaEntity toEntity(SubtitleTrack domain);

    // the key, the lock version and the audit stamps belong to the stored row
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "videoLesson", source = "videoLessonId")
    void update(@MappingTarget SubtitleTrackJpaEntity entity, SubtitleTrack domain);
}
