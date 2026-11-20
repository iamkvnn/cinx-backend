package com.cinx.commerce.subscription.api.dto;

import jakarta.validation.constraints.NotBlank;

public record CheckoutSessionRequest(@NotBlank String offerId, @NotBlank String returnPath) {
}
