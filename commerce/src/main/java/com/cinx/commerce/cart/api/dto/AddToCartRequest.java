package com.cinx.commerce.cart.api.dto;

import jakarta.validation.constraints.NotBlank;

public record AddToCartRequest(@NotBlank String courseId) {
}
