package com.cinx.commerce.wishlist.infrastructure.persistence;

import com.cinx.commerce.wishlist.domain.WishlistItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
interface WishlistItemPersistenceMapper {
    WishlistItemJpaEntity toEntity(WishlistItem domain);

    WishlistItem toDomain(WishlistItemJpaEntity entity);

    void update(@MappingTarget WishlistItemJpaEntity entity, WishlistItem domain);
}
