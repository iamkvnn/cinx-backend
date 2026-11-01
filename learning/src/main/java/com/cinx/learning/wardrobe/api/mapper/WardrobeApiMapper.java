package com.cinx.learning.wardrobe.api.mapper;

import com.cinx.learning.wardrobe.api.dto.EquipOwlSkinsRequest;
import com.cinx.learning.wardrobe.api.dto.OwlSkinResponse;
import com.cinx.learning.wardrobe.api.dto.OwlWardrobeResponse;
import com.cinx.learning.wardrobe.api.dto.UpdateOwlSkinRequest;
import com.cinx.learning.wardrobe.application.command.EquipOwlSkinsCommand;
import com.cinx.learning.wardrobe.application.command.UpdateOwlSkinCommand;
import com.cinx.learning.wardrobe.application.query.OwlSkinView;
import com.cinx.learning.wardrobe.application.query.OwlWardrobeView;
import org.mapstruct.Mapper;

/** Converts the api's transport types to the application's commands and the application's views to transport types. */
@Mapper(componentModel = "spring")
public interface WardrobeApiMapper {
    OwlWardrobeResponse toResponse(OwlWardrobeView view);

    OwlWardrobeResponse.Equipped toResponse(OwlWardrobeView.Equipped view);

    OwlWardrobeResponse.Item toResponse(OwlWardrobeView.Item view);

    OwlSkinResponse toResponse(OwlSkinView view);

    EquipOwlSkinsCommand toCommand(EquipOwlSkinsRequest request);

    UpdateOwlSkinCommand toCommand(UpdateOwlSkinRequest request);
}
