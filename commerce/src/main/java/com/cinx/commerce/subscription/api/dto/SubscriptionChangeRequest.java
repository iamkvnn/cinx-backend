package com.cinx.commerce.subscription.api.dto;

import jakarta.validation.constraints.NotBlank;

public record SubscriptionChangeRequest(@NotBlank String offerId) {
}
