package com.cinx.learning.catalog.application.mapper;

import com.cinx.common.mapper.BaseMapper;
import com.cinx.common.mapper.CreateMapper;
import com.cinx.common.mapper.UpdateMapper;
import com.cinx.learning.catalog.application.command.CreateVideoQuestionCommand;
import com.cinx.learning.catalog.application.command.UpdateVideoQuestionCommand;
import com.cinx.learning.catalog.application.query.VideoQuestionView;
import com.cinx.learning.catalog.domain.VideoQuestion;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface VideoQuestionMapper extends BaseMapper<VideoQuestion, VideoQuestionView>,
        CreateMapper<VideoQuestion, CreateVideoQuestionCommand>,
        UpdateMapper<VideoQuestion, UpdateVideoQuestionCommand> {
}
