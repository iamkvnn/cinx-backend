package com.cinx.learning.wardrobe.application.command;

import java.time.LocalDate;

/** An admin change to one catalog item; a null field is left as it is. Dates apply to season skins only. */
public record UpdateOwlSkinCommand(
        Boolean active,
        LocalDate seasonStartsOn,
        LocalDate seasonEndsOn
) {
}
