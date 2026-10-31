package com.cinx.learning.progress.application.mapper;

import com.cinx.common.mapper.BaseMapper;
import com.cinx.common.mapper.UpdateMapper;
import com.cinx.learning.progress.application.command.UpdateLearningItemCommand;
import com.cinx.learning.progress.application.query.LearningItemProgressView;
import com.cinx.learning.progress.domain.LearningItemProgress;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface LearningItemProgressMapper extends
        BaseMapper<LearningItemProgress, LearningItemProgressView>,
        UpdateMapper<LearningItemProgress, UpdateLearningItemCommand> {
}
