package com.cinx.reporting.analytics.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.Map;

public record ProductEventRequest(
        @Schema(description = "A UUID made by the client; sending it again is a no-op", example = "3f1c2a9e-8f7b-4c2d-9a51-0d4b7e6f1a20")
        @NotBlank @Size(max = 36)
        String eventId,
        @Schema(example = "recommendation_clicked")
        @NotBlank @Size(max = 64)
        String name,
        @Schema(description = "Client time, with an offset")
        @NotNull
        OffsetDateTime occurredAt,
        @Schema(description = "Only the props the dictionary lists for this event are kept")
        Map<String, Object> props
) {
}
