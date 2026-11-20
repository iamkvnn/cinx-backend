package com.cinx.learning.catalog.application.mapper;

import com.cinx.common.mapper.BaseMapper;
import com.cinx.common.mapper.CreateMapper;
import com.cinx.common.mapper.UpdateMapper;
import com.cinx.learning.catalog.application.command.CreateVideoOptionCommand;
import com.cinx.learning.catalog.application.command.UpdateVideoOptionCommand;
import com.cinx.learning.catalog.application.query.VideoOptionView;
import com.cinx.learning.catalog.domain.VideoOption;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface VideoOptionMapper extends BaseMapper<VideoOption, VideoOptionView>,
        CreateMapper<VideoOption, CreateVideoOptionCommand>,
        UpdateMapper<VideoOption, UpdateVideoOptionCommand> {
        
    VideoOption toModel(UpdateVideoOptionCommand request);
}
