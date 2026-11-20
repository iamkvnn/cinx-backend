package com.cinx.commerce.catalog.infrastructure.persistence;

import com.cinx.commerce.catalog.domain.CommerceFeatureRegistry;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
interface CommerceFeatureRegistryPersistenceMapper {
    CommerceFeatureRegistryJpaEntity toEntity(CommerceFeatureRegistry domain);

    CommerceFeatureRegistry toDomain(CommerceFeatureRegistryJpaEntity entity);

    void update(@MappingTarget CommerceFeatureRegistryJpaEntity entity, CommerceFeatureRegistry domain);
}
