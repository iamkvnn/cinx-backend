package com.cinx.learning.wardrobe.infrastructure.persistence;

import com.cinx.learning.wardrobe.domain.OwlWardrobe;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface OwlWardrobePersistenceMapper {
    OwlWardrobe toDomain(OwlWardrobeJpaEntity entity);

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    OwlWardrobeJpaEntity toEntity(OwlWardrobe domain);

    // the key, the owner, the lock version and the audit stamps belong to the stored row
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "userId", ignore = true)
    void update(@MappingTarget OwlWardrobeJpaEntity entity, OwlWardrobe domain);
}
