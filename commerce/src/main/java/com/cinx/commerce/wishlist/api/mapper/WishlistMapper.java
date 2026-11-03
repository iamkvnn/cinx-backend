package com.cinx.commerce.wishlist.api.mapper;

import com.cinx.commerce.wishlist.api.dto.WishlistResponse;
import com.cinx.commerce.wishlist.application.query.WishlistEntryView;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface WishlistMapper {
    WishlistResponse toResponse(WishlistEntryView view);
}
