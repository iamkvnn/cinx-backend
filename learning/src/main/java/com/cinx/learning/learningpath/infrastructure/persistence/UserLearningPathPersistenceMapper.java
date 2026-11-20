package com.cinx.learning.learningpath.infrastructure.persistence;

import com.cinx.learning.learningpath.domain.UserLearningPath;
import org.mapstruct.AfterMapping;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR, uses = {LearningPathItemPersistenceMapper.class})
interface UserLearningPathPersistenceMapper {
    UserLearningPath toDomain(UserLearningPathJpaEntity entity);

    UserLearningPathJpaEntity toEntity(UserLearningPath domain);

    // the key, the lock version and the audit stamps belong to the stored row
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "items", ignore = true)
    void update(@MappingTarget UserLearningPathJpaEntity entity, UserLearningPath domain);

    @AfterMapping
    default void linkItems(@MappingTarget UserLearningPathJpaEntity entity) {
        entity.getItems().forEach(child -> child.setLearningPath(entity));
    }
}
