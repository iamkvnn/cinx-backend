package com.cinx.learning.community.infrastructure.persistence;

import com.cinx.learning.community.domain.Review;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface ReviewPersistenceMapper {
    Review toDomain(ReviewJpaEntity entity);

    ReviewJpaEntity toEntity(Review domain);

    // the key, the lock version and the audit stamps belong to the stored row
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "reactions", ignore = true)
    void update(@MappingTarget ReviewJpaEntity entity, Review domain);
}
