package com.cinx.commerce.catalog.api.dto;

import java.util.List;

/**
 * A published plan. {@code name} falls back to the plan code when no translation exists;
 * {@code purchasable} is false while billing is off or no provider is configured.
 */
public record PublishedPlanResponse(
        String code,
        String productLine,
        long version,
        String defaultLocale,
        String name,
        String description,
        String badge,
        boolean purchasable,
        List<PlanFeatureResponse> features,
        List<PlanOfferResponse> offers) {
}
