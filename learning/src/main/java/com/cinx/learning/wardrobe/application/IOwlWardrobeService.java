package com.cinx.learning.wardrobe.application;

import com.cinx.learning.wardrobe.application.command.EquipOwlSkinsCommand;
import com.cinx.learning.wardrobe.application.command.UpdateOwlSkinCommand;
import com.cinx.learning.wardrobe.application.query.OwlSkinView;
import com.cinx.learning.wardrobe.application.query.OwlWardrobeView;

import java.util.List;

public interface IOwlWardrobeService {
    OwlWardrobeView getWardrobe(String userId);

    /** Wears the given skins; each must be active, of its slot and unlocked for the learner. */
    OwlWardrobeView equip(String userId, EquipOwlSkinsCommand command);

    /** Grants every open season skin the learner lacks; called when a lesson becomes complete. */
    void grantOpenSeasonSkins(String userId);

    List<OwlSkinView> getCatalog();

    OwlSkinView updateSkin(String code, UpdateOwlSkinCommand command);
}
