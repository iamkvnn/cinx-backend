package com.cinx.learning.catalog.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record UpdateLessonRequest(
     @NotBlank
     @Schema(example = "Advanced Configuration in Spring Boot")
     String title,
     @Min(0)
     @Schema(example = "800")
     Long duration,
     @Schema(example = "false")
     Boolean isPreview,
     @Schema(example = "[\"les_122\", \"les_123\"]")
     List<String> prerequisiteIds
) {
}


