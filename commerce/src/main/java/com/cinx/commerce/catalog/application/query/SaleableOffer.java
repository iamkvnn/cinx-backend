package com.cinx.commerce.catalog.application.query;

/** An offer a learner may subscribe to right now; {@code priceId} is null for a free offer. */
public record SaleableOffer(String offerId, String priceId) {
}
