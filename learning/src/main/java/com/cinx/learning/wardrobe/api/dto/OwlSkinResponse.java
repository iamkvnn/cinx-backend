package com.cinx.learning.wardrobe.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

public record OwlSkinResponse(
        @Schema(example = "non-la")
        String code,
        @Schema(example = "HEAD", allowableValues = {"PALETTE", "HEAD", "FACE", "EFFECT", "HOLD"})
        String slot,
        @Schema(example = "SEASON", allowableValues = {"FREE", "STREAK", "SEASON"})
        String unlockRule,
        @Schema(description = "STREAK items only", example = "7")
        Integer streakDays,
        @Schema(description = "SEASON items only, inclusive, Asia/Ho_Chi_Minh", example = "2027-01-30")
        LocalDate seasonStartsOn,
        @Schema(description = "SEASON items only, inclusive, Asia/Ho_Chi_Minh", example = "2027-02-14")
        LocalDate seasonEndsOn,
        @Schema(example = "Nón lá")
        String name,
        @Schema(example = "true")
        Boolean active,
        @Schema(example = "7")
        Integer sortOrder
) {
}
