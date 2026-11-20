package com.cinx.learning.studyplan.api.mapper;

import com.cinx.learning.studyplan.api.dto.request.UpsertStudyPlanRequest;
import com.cinx.learning.studyplan.api.dto.response.DayOffResponse;
import com.cinx.learning.studyplan.api.dto.response.LastActivityResponse;
import com.cinx.learning.studyplan.api.dto.response.StudyPlanPreviewResponse;
import com.cinx.learning.studyplan.api.dto.response.StudyPlanResponse;
import com.cinx.learning.studyplan.application.command.UpsertStudyPlanCommand;
import com.cinx.learning.studyplan.application.query.DayOffView;
import com.cinx.learning.studyplan.application.query.LastActivityView;
import com.cinx.learning.studyplan.application.query.StudyPlanPreviewView;
import com.cinx.learning.studyplan.application.query.StudyPlanView;
import org.mapstruct.Mapper;

/** Converts the api's transport types to the application's commands and the application's views to transport types. */
@Mapper(componentModel = "spring")
public interface StudyplanApiMapper {
    DayOffResponse toResponse(DayOffView view);
    LastActivityResponse toResponse(LastActivityView view);
    StudyPlanPreviewResponse toResponse(StudyPlanPreviewView view);
    StudyPlanResponse toResponse(StudyPlanView view);
    UpsertStudyPlanCommand toCommand(UpsertStudyPlanRequest request);
    com.cinx.learning.studyplan.api.dto.response.StudyPlanResponse.Course toResponse(com.cinx.learning.studyplan.application.query.StudyPlanView.Course view);
    com.cinx.learning.studyplan.api.dto.response.StudyPlanResponse.Week toResponse(com.cinx.learning.studyplan.application.query.StudyPlanView.Week view);
    com.cinx.learning.studyplan.api.dto.response.StudyPlanResponse.Day toResponse(com.cinx.learning.studyplan.application.query.StudyPlanView.Day view);
    com.cinx.learning.studyplan.api.dto.response.StudyPlanResponse.Session toResponse(com.cinx.learning.studyplan.application.query.StudyPlanView.Session view);
}
