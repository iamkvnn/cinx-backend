package com.cinx.learning.wardrobe.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "A change to one catalog item; omitted fields stay as they are. Dates apply to SEASON items only")
public record UpdateOwlSkinRequest(
        @Schema(example = "true")
        Boolean active,
        @Schema(description = "Inclusive, Asia/Ho_Chi_Minh", example = "2027-01-30")
        LocalDate seasonStartsOn,
        @Schema(description = "Inclusive, Asia/Ho_Chi_Minh", example = "2027-02-14")
        LocalDate seasonEndsOn
) {
}
