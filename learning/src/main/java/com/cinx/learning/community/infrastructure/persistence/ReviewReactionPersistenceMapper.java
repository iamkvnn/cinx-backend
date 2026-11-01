package com.cinx.learning.community.infrastructure.persistence;

import com.cinx.learning.community.domain.ReviewReaction;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface ReviewReactionPersistenceMapper {
    ReviewReaction toDomain(ReviewReactionJpaEntity entity);

    ReviewReactionJpaEntity toEntity(ReviewReaction domain);

    // the key, the lock version and the audit stamps belong to the stored row
    @Mapping(target = "id", ignore = true)
    void update(@MappingTarget ReviewReactionJpaEntity entity, ReviewReaction domain);
}
