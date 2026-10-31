package com.cinx.learning.learningpath.api.mapper;

import com.cinx.learning.learningpath.api.dto.request.LearningPathItemRequest;
import com.cinx.learning.learningpath.api.dto.request.LearningPathItemsUpdateRequest;
import com.cinx.learning.learningpath.api.dto.request.LearningPathRequest;
import com.cinx.learning.learningpath.api.dto.response.LearningPathItemResponse;
import com.cinx.learning.learningpath.api.dto.response.LearningPathResponse;
import com.cinx.learning.learningpath.application.command.LearningPathCommand;
import com.cinx.learning.learningpath.application.command.LearningPathItemCommand;
import com.cinx.learning.learningpath.application.command.LearningPathItemsUpdateCommand;
import com.cinx.learning.learningpath.application.query.LearningPathItemView;
import com.cinx.learning.learningpath.application.query.LearningPathView;
import org.mapstruct.Mapper;

/** Converts the api's transport types to the application's commands and the application's views to transport types. */
@Mapper(componentModel = "spring")
public interface LearningpathApiMapper {
    LearningPathItemCommand toCommand(LearningPathItemRequest request);
    LearningPathItemResponse toResponse(LearningPathItemView view);
    LearningPathItemsUpdateCommand toCommand(LearningPathItemsUpdateRequest request);
    LearningPathCommand toCommand(LearningPathRequest request);
    LearningPathResponse toResponse(LearningPathView view);
}
