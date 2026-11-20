package com.cinx.learning.wardrobe.infrastructure.persistence;

import com.cinx.learning.wardrobe.domain.OwlSkinGrant;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface OwlSkinGrantPersistenceMapper {
    OwlSkinGrant toDomain(OwlSkinGrantJpaEntity entity);

    OwlSkinGrantJpaEntity toEntity(OwlSkinGrant domain);
}
