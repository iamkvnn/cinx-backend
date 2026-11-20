package com.cinx.commerce.wishlist.api.dto;

import jakarta.validation.constraints.NotBlank;

public record WishlistRequest(@NotBlank String courseId) {
}
