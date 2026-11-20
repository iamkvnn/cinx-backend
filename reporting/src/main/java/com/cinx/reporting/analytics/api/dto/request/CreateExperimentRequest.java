package com.cinx.reporting.analytics.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

public record CreateExperimentRequest(
        @Schema(example = "week_review_card", description = "3-64 of a-z, 0-9 and _; the client asks for it by this key")
        @NotBlank @Size(max = 64)
        String key,
        @NotBlank @Size(max = 500)
        String description,
        @Schema(description = "2-5 variants, one called control; weights add up to 100")
        @NotNull @Size(min = 2, max = 5)
        List<@NotNull @Valid VariantRequest> variants,
        @Schema(example = "learning_action_completed")
        @NotBlank @Size(max = 64)
        String goalEvent,
        @Schema(example = "{\"action\":\"review_answered\"}")
        Map<String, String> goalFilter,
        @Min(1) @Max(90)
        int goalWindowDays
) {
    public record VariantRequest(
            @Schema(example = "hidden") @NotBlank @Size(max = 64) String key,
            @Schema(example = "50") @Min(1) @Max(99) int weight) {
    }
}
