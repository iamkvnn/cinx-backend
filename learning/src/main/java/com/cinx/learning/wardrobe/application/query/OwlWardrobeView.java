package com.cinx.learning.wardrobe.application.query;

import com.cinx.learning.wardrobe.domain.OwlSkinSlot;
import com.cinx.learning.wardrobe.domain.OwlSkinUnlockRule;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** One learner's wardrobe: what the owl wears, every active item with whether it is theirs, and their best streak. */
public record OwlWardrobeView(
        Equipped equipped,
        List<Item> items,
        int highestStreak
) {
    public record Equipped(
            String palette,
            String head,
            String face,
            String effect,
            String hold
    ) {
    }

    public record Item(
            String code,
            OwlSkinSlot slot,
            OwlSkinUnlockRule unlockRule,
            Integer streakDays,
            LocalDate seasonStartsOn,
            LocalDate seasonEndsOn,
            boolean seasonOpen,
            boolean unlocked,
            LocalDateTime unlockedAt
    ) {
    }
}
