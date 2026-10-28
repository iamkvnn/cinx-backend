package com.cinx.learning.catalog.infrastructure.persistence;

import com.cinx.learning.catalog.domain.SubtitleJob;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR, uses = {CatalogEntityRefs.class})
interface SubtitleJobPersistenceMapper {
    @Mapping(target = "videoLessonId", source = "videoLesson.lessonId")
    SubtitleJob toDomain(SubtitleJobJpaEntity entity);

    @Mapping(target = "videoLesson", source = "videoLessonId")
    SubtitleJobJpaEntity toEntity(SubtitleJob domain);

    // the key, the lock version and the audit stamps belong to the stored row
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "videoLesson", source = "videoLessonId")
    void update(@MappingTarget SubtitleJobJpaEntity entity, SubtitleJob domain);
}
