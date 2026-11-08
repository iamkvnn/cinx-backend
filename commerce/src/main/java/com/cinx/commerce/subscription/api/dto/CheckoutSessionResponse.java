package com.cinx.commerce.subscription.api.dto;

import java.time.Instant;

public record CheckoutSessionResponse(String commandId, String checkoutUrl, Instant expiresAt, String status) {
}
