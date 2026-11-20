package com.cinx.learning.wardrobe.application.query;

import com.cinx.learning.wardrobe.domain.OwlSkinSlot;
import com.cinx.learning.wardrobe.domain.OwlSkinUnlockRule;

import java.time.LocalDate;

/** A catalog item as admins see it. */
public record OwlSkinView(
        String code,
        OwlSkinSlot slot,
        OwlSkinUnlockRule unlockRule,
        Integer streakDays,
        LocalDate seasonStartsOn,
        LocalDate seasonEndsOn,
        String name,
        Boolean active,
        Integer sortOrder
) {
}
