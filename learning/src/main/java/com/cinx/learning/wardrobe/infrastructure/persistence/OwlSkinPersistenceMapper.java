package com.cinx.learning.wardrobe.infrastructure.persistence;

import com.cinx.learning.wardrobe.domain.OwlSkin;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface OwlSkinPersistenceMapper {
    OwlSkin toDomain(OwlSkinJpaEntity entity);

    OwlSkinJpaEntity toEntity(OwlSkin domain);

    // the key, the lock version and the audit stamps belong to the stored row
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    void update(@MappingTarget OwlSkinJpaEntity entity, OwlSkin domain);
}
