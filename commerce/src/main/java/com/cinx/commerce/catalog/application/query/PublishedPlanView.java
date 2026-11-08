package com.cinx.commerce.catalog.application.query;

import java.math.BigDecimal;
import java.util.List;

/**
 * A plan as customers and administrators see it. {@code name} falls back to the plan code when no translation exists;
 * {@code purchasable} is false while billing is off or no provider is configured.
 */
public record PublishedPlanView(
        String code,
        String productLine,
        long version,
        String defaultLocale,
        String name,
        String description,
        String badge,
        boolean purchasable,
        List<Feature> features,
        List<Offer> offers) {

    /** One saleable feature of a plan: a switch (BOOLEAN) or a quota with its unit and reset period. */
    public record Feature(String key, String valueType, boolean enabled, Long quotaLimit, boolean quotaUnlimited,
                          String quotaUnit, String resetPolicy) {
    }

    /** An offer of a plan. The price fields are null for a free offer. */
    public record Offer(String offerId, String market, String priceId, String status, BigDecimal amount, String currency,
                        String billingInterval, Integer intervalCount) {
    }
}
