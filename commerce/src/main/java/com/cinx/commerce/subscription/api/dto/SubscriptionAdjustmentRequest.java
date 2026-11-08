package com.cinx.commerce.subscription.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.Map;

public record SubscriptionAdjustmentRequest(
        String featureKey,
        Map<String, Object> value,
        @NotBlank String reason,
        @NotNull LocalDateTime effectiveFrom,
        LocalDateTime expiresAt
) {
}
