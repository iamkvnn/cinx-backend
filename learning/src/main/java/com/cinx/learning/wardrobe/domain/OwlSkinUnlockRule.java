package com.cinx.learning.wardrobe.domain;

/**
 * How a learner gets an owl skin. FREE skins are open to everyone; STREAK skins open once the learner's highest streak
 * reaches the skin's days; SEASON skins are granted by completing a lesson while the season is open, and kept.
 */
public enum OwlSkinUnlockRule {
    FREE,
    STREAK,
    SEASON
}
