package com.cinx.commerce.catalog.api.dto;

import jakarta.validation.constraints.NotBlank;

public record PlanDraftRequest(
        @NotBlank String code,
        @NotBlank String productLine,
        String defaultLocale) {
}
