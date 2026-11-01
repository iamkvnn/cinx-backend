package com.cinx.learning.engagement.api.mapper;

import com.cinx.learning.engagement.api.dto.request.LearningActivityRequest;
import com.cinx.learning.engagement.api.dto.request.SetDailyGoalRequest;
import com.cinx.learning.engagement.api.dto.response.CourseEngagementOverviewResponse;
import com.cinx.learning.engagement.api.dto.response.LearningActivityByTimeResponse;
import com.cinx.learning.engagement.api.dto.response.LearningActivitySummaryResponse;
import com.cinx.learning.engagement.api.dto.response.UserLearningSummaryResponse;
import com.cinx.learning.engagement.api.dto.response.UserStreakResponse;
import com.cinx.learning.engagement.application.command.LearningActivityCommand;
import com.cinx.learning.engagement.application.command.SetDailyGoalCommand;
import com.cinx.learning.engagement.application.query.CourseEngagementOverviewView;
import com.cinx.learning.engagement.application.query.LearningActivityByTimeView;
import com.cinx.learning.engagement.application.query.LearningActivitySummaryView;
import com.cinx.learning.engagement.application.query.UserLearningSummaryView;
import com.cinx.learning.engagement.application.query.UserStreakView;
import com.cinx.learning.engagement.api.dto.response.DailyGoalResponse;
import com.cinx.learning.engagement.application.query.DailyGoalView;
import org.mapstruct.Mapper;

/** Converts the api's transport types to the application's commands and the application's views to transport types. */
@Mapper(componentModel = "spring")
public interface EngagementApiMapper {
    CourseEngagementOverviewResponse toResponse(CourseEngagementOverviewView view);
    LearningActivityByTimeResponse toResponse(LearningActivityByTimeView view);
    LearningActivityCommand toCommand(LearningActivityRequest request);
    LearningActivitySummaryResponse toResponse(LearningActivitySummaryView view);
    SetDailyGoalCommand toCommand(SetDailyGoalRequest request);
    UserLearningSummaryResponse toResponse(UserLearningSummaryView view);
    UserStreakResponse toResponse(UserStreakView view);
    DailyGoalResponse toResponse(DailyGoalView view);
    com.cinx.learning.engagement.api.dto.response.LearningActivitySummaryResponse.Day toResponse(com.cinx.learning.engagement.application.query.LearningActivitySummaryView.Day view);
}
