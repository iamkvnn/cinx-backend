package com.cinx.reporting.analytics.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record ExperimentResponse(
        String key,
        String description,
        @Schema(description = "DRAFT, RUNNING or STOPPED; only RUNNING assigns anyone outside control")
        String status,
        List<VariantWeight> variants,
        String goalEvent,
        Map<String, String> goalFilter,
        int goalWindowDays,
        LocalDateTime startedAt,
        LocalDateTime stoppedAt,
        String updatedBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public record VariantWeight(String key, int weight) {
    }
}
