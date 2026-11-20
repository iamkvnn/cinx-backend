package com.cinx.learning.instructor.api.mapper;

import com.cinx.learning.instructor.api.dto.ActivityResponse;
import com.cinx.learning.instructor.api.dto.CourseLearnersResponse;
import com.cinx.learning.instructor.api.dto.InboxResponse;
import com.cinx.learning.instructor.api.dto.LessonReachResponse;
import com.cinx.learning.instructor.api.dto.OverviewResponse;
import com.cinx.learning.instructor.application.query.ActivityView;
import com.cinx.learning.instructor.application.query.CourseLearnersView;
import com.cinx.learning.instructor.application.query.InboxView;
import com.cinx.learning.instructor.application.query.LessonReachView;
import com.cinx.learning.instructor.application.query.OverviewView;
import org.mapstruct.Mapper;

/** Converts the api's transport types to the application's commands and the application's views to transport types. */
@Mapper(componentModel = "spring")
public interface InstructorApiMapper {
    ActivityResponse toResponse(ActivityView view);
    CourseLearnersResponse toResponse(CourseLearnersView view);
    InboxResponse toResponse(InboxView view);
    com.cinx.learning.instructor.api.dto.InstructorCourseRow toResponse(com.cinx.learning.instructor.application.query.InstructorCourseRow view);
    LessonReachResponse toResponse(LessonReachView view);
    OverviewResponse toResponse(OverviewView view);
    com.cinx.learning.instructor.api.dto.QuizSummary toResponse(com.cinx.learning.instructor.application.query.QuizSummary view);
    com.cinx.learning.instructor.application.query.CommentTemplateDtos.CreateRequest toCommand(com.cinx.learning.instructor.api.dto.CommentTemplateDtos.CreateRequest request);
    com.cinx.learning.instructor.api.dto.CommentTemplateDtos.Response toResponse(com.cinx.learning.instructor.application.query.CommentTemplateDtos.Response view);
    com.cinx.learning.instructor.api.dto.CourseLearnersResponse.Row toResponse(com.cinx.learning.instructor.application.query.CourseLearnersView.Row view);
    com.cinx.learning.instructor.api.dto.CourseLearnersResponse.Counts toResponse(com.cinx.learning.instructor.application.query.CourseLearnersView.Counts view);
    com.cinx.learning.instructor.api.dto.InboxResponse.Sources toResponse(com.cinx.learning.instructor.application.query.InboxView.Sources view);
    com.cinx.learning.instructor.api.dto.InboxResponse.CourseWork toResponse(com.cinx.learning.instructor.application.query.InboxView.CourseWork view);
    com.cinx.learning.instructor.api.dto.InboxResponse.Group toResponse(com.cinx.learning.instructor.application.query.InboxView.Group view);
    com.cinx.learning.instructor.api.dto.InboxResponse.Summary toResponse(com.cinx.learning.instructor.application.query.InboxView.Summary view);
    com.cinx.learning.instructor.api.dto.LessonReachResponse.Lesson toResponse(com.cinx.learning.instructor.application.query.LessonReachView.Lesson view);
    com.cinx.learning.instructor.api.dto.OverviewResponse.DayCount toResponse(com.cinx.learning.instructor.application.query.OverviewView.DayCount view);
    com.cinx.learning.instructor.api.dto.OverviewResponse.CourseRow toResponse(com.cinx.learning.instructor.application.query.OverviewView.CourseRow view);
}
