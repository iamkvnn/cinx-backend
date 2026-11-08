package com.cinx.commerce.catalog.api.dto;

import java.math.BigDecimal;

/** An offer of a plan. The price fields are null for a free offer. */
public record PlanOfferResponse(String offerId, String market, String priceId, String status,
                                BigDecimal amount, String currency, String billingInterval, Integer intervalCount) {
}
