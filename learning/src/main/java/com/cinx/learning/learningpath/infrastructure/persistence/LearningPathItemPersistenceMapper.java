package com.cinx.learning.learningpath.infrastructure.persistence;

import com.cinx.learning.learningpath.domain.LearningPathItem;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR, uses = {LearningpathEntityRefs.class})
interface LearningPathItemPersistenceMapper {
    @Mapping(target = "learningPathId", source = "learningPath.id")
    LearningPathItem toDomain(LearningPathItemJpaEntity entity);

    @Mapping(target = "learningPath", source = "learningPathId")
    LearningPathItemJpaEntity toEntity(LearningPathItem domain);

    // the key, the lock version and the audit stamps belong to the stored row
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "learningPath", source = "learningPathId")
    void update(@MappingTarget LearningPathItemJpaEntity entity, LearningPathItem domain);
}
