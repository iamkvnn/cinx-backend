package com.cinx.commerce.cart.infrastructure.persistence;

import com.cinx.commerce.cart.domain.CartItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
interface CartItemPersistenceMapper {
    CartItemJpaEntity toEntity(CartItem domain);

    CartItem toDomain(CartItemJpaEntity entity);

    void update(@MappingTarget CartItemJpaEntity entity, CartItem domain);
}
