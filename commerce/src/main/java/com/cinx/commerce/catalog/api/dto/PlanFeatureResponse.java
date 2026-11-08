package com.cinx.commerce.catalog.api.dto;

/** One saleable feature of a plan: a switch (BOOLEAN) or a quota with its unit and reset period. */
public record PlanFeatureResponse(
        String key,
        String valueType,
        boolean enabled,
        Long quotaLimit,
        boolean quotaUnlimited,
        String quotaUnit,
        String resetPolicy) {
}
