package com.cinx.commerce.cart.api.mapper;

import com.cinx.commerce.cart.api.dto.CartItemResponse;
import com.cinx.commerce.cart.application.query.CartItemView;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CartMapper {
    CartItemResponse toResponse(CartItemView view);
}
