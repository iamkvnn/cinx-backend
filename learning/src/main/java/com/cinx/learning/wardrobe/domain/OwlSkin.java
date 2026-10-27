package com.cinx.learning.wardrobe.domain;

import com.cinx.learning.shared.domain.AuditedModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

/**
 * One item of the tutor owl's wardrobe. Its art lives in the web app's Rive file under the same code, so the catalog
 * is seeded with a release; admins only switch items on or off and date the seasons.
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class OwlSkin extends AuditedModel {
    private String code;
    private OwlSkinSlot slot;
    private OwlSkinUnlockRule unlockRule;
    private Integer streakDays;
    private LocalDate seasonStartsOn;
    private LocalDate seasonEndsOn;
    private String name;
    private Integer sortOrder;
    @Builder.Default
    private Boolean active = true;

    /** True while a season skin can be earned: the day falls inside its inclusive window. */
    public boolean isSeasonOpenOn(LocalDate day) {
        return unlockRule == OwlSkinUnlockRule.SEASON && seasonStartsOn != null && seasonEndsOn != null
                && !day.isBefore(seasonStartsOn) && !day.isAfter(seasonEndsOn);
    }
}
