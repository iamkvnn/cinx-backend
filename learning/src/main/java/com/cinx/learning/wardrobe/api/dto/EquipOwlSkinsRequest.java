package com.cinx.learning.wardrobe.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "The skin code to wear in each slot; null or empty wears the default look there")
public record EquipOwlSkinsRequest(
        @Size(max = 40) @Schema(example = "snow")
        String palette,
        @Size(max = 40) @Schema(example = "grad-cap")
        String head,
        @Size(max = 40) @Schema(example = "round-glasses")
        String face,
        @Size(max = 40) @Schema(example = "sparkle-trail")
        String effect,
        @Size(max = 40) @Schema(example = "book")
        String hold
) {
}
