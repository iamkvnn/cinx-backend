package com.cinx.reporting.analytics.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "Up to 50 product events of the signed-in user; the dictionary is contracts/analytics/product-events.yaml")
public record ProductEventBatchRequest(
        @NotEmpty @Size(max = 50)
        List<@NotNull @Valid ProductEventRequest> events
) {
}
