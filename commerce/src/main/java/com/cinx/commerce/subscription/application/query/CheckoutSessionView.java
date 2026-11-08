package com.cinx.commerce.subscription.application.query;

import java.time.Instant;

/** Where a learner pays for a subscription, and how far the command stands. The URL is null for a free offer. */
public record CheckoutSessionView(String commandId, String checkoutUrl, Instant expiresAt, String status) {
}
