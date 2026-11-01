package com.cinx.learning.wardrobe.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record OwlWardrobeResponse(
        Equipped equipped,
        @Schema(description = "Every active item, with whether the caller may wear it")
        List<Item> items,
        @Schema(description = "The caller's best streak; streak items open at their days", example = "12")
        int highestStreak
) {
    @Schema(description = "Skin code worn per slot; null is the default look")
    public record Equipped(
            @Schema(example = "snow")
            String palette,
            @Schema(example = "grad-cap")
            String head,
            @Schema(example = "round-glasses")
            String face,
            @Schema(example = "sparkle-trail")
            String effect,
            @Schema(example = "book")
            String hold
    ) {
    }

    public record Item(
            @Schema(example = "round-glasses")
            String code,
            @Schema(example = "FACE", allowableValues = {"PALETTE", "HEAD", "FACE", "EFFECT", "HOLD"})
            String slot,
            @Schema(example = "STREAK", allowableValues = {"FREE", "STREAK", "SEASON"})
            String unlockRule,
            @Schema(description = "STREAK items only", example = "3")
            Integer streakDays,
            @Schema(description = "SEASON items only, inclusive, Asia/Ho_Chi_Minh", example = "2027-01-30")
            LocalDate seasonStartsOn,
            @Schema(description = "SEASON items only, inclusive, Asia/Ho_Chi_Minh", example = "2027-02-14")
            LocalDate seasonEndsOn,
            @Schema(description = "A SEASON item can be earned today by completing a lesson")
            boolean seasonOpen,
            boolean unlocked,
            @Schema(description = "When a SEASON item was earned (UTC); null otherwise")
            LocalDateTime unlockedAt
    ) {
    }
}
