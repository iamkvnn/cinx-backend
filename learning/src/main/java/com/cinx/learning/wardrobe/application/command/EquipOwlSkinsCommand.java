package com.cinx.learning.wardrobe.application.command;

/** The skin code to wear in each slot; null wears the default look there. */
public record EquipOwlSkinsCommand(
        String palette,
        String head,
        String face,
        String effect
) {
}
