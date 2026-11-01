package com.cinx.learning.dailyquiz.api.mapper;

import com.cinx.learning.dailyquiz.api.dto.DailyQuizResponse;
import com.cinx.learning.dailyquiz.api.dto.UpsertDailyQuizRequest;
import com.cinx.learning.dailyquiz.application.command.UpsertDailyQuizCommand;
import com.cinx.learning.dailyquiz.application.query.DailyQuizView;
import org.mapstruct.Mapper;

/** Converts the api's transport types to the application's commands and the application's views to transport types. */
@Mapper(componentModel = "spring")
public interface DailyquizApiMapper {
    DailyQuizResponse toResponse(DailyQuizView view);
    UpsertDailyQuizCommand toCommand(UpsertDailyQuizRequest request);
    com.cinx.learning.dailyquiz.api.dto.DailyQuizResponse.Option toResponse(com.cinx.learning.dailyquiz.application.query.DailyQuizView.Option view);
    com.cinx.learning.dailyquiz.application.command.UpsertDailyQuizCommand.Option toCommand(com.cinx.learning.dailyquiz.api.dto.UpsertDailyQuizRequest.Option request);
}
