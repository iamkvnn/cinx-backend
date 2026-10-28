package com.cinx.learning.catalog.infrastructure.persistence;

import com.cinx.learning.catalog.domain.VideoOption;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR, uses = {CatalogEntityRefs.class})
interface VideoOptionPersistenceMapper {
    @Mapping(target = "videoQuestionId", source = "videoQuestion.id")
    VideoOption toDomain(VideoOptionJpaEntity entity);

    @Mapping(target = "videoQuestion", source = "videoQuestionId")
    VideoOptionJpaEntity toEntity(VideoOption domain);

    // the key, the lock version and the audit stamps belong to the stored row
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "videoQuestion", source = "videoQuestionId")
    void update(@MappingTarget VideoOptionJpaEntity entity, VideoOption domain);
}
