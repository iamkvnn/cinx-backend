package com.cinx.learning.featured.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpsertFeaturedPathRequest(
        @NotBlank @Size(max = 120)
        @Schema(example = "Backend Java")
        String name,

        @NotBlank @Size(max = 120)
        @Schema(example = "Backend Developer")
        String roleTitle,

        @NotNull @Min(1) @Max(36)
        @Schema(example = "4")
        Integer estimatedMonths,

        @NotNull @Min(1) @Max(40)
        @Schema(example = "5")
        Integer hoursPerWeek,

        @Schema(example = "true")
        Boolean active,

        @NotEmpty @Size(max = 12, message = "A featured path has 1 to 12 courses")
        @Schema(description = "Published course ids, in learning order")
        List<@NotBlank String> courseIds,

        @Size(max = 500)
        @Schema(description = "Shown under the title on the public path page")
        String description,

        @Size(max = 5, message = "A featured path has at most 5 outcomes")
        @Schema(description = "\"After this path you can …\" lines, in display order")
        List<@NotBlank @Size(max = 120) String> outcomes
) {
}
