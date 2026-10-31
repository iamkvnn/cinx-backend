package com.cinx.learning.progress.api.mapper;

import com.cinx.learning.progress.api.dto.request.TrackingVideoLessonRequest;
import com.cinx.learning.progress.api.dto.request.UpdateCourseProgressRequest;
import com.cinx.learning.progress.api.dto.request.UpdateLearningItemRequest;
import com.cinx.learning.progress.api.dto.response.CourseProgressResponse;
import com.cinx.learning.progress.api.dto.response.CourseProgressSummaryResponse;
import com.cinx.learning.progress.api.dto.response.CoursesProgressSummaryResponse;
import com.cinx.learning.progress.api.dto.response.LearningItemProgressResponse;
import com.cinx.learning.progress.api.dto.response.VideoLessonTrackingHistoryResponse;
import com.cinx.learning.progress.application.command.TrackingVideoLessonCommand;
import com.cinx.learning.progress.application.command.UpdateCourseProgressCommand;
import com.cinx.learning.progress.application.command.UpdateLearningItemCommand;
import com.cinx.learning.progress.application.query.CourseProgressSummaryView;
import com.cinx.learning.progress.application.query.CourseProgressView;
import com.cinx.learning.progress.application.query.CoursesProgressSummaryView;
import com.cinx.learning.progress.application.query.LearningItemProgressView;
import com.cinx.learning.progress.application.query.VideoLessonTrackingHistoryView;
import org.mapstruct.Mapper;

/** Converts the api's transport types to the application's commands and the application's views to transport types. */
@Mapper(componentModel = "spring")
public interface ProgressApiMapper {
    CourseProgressResponse toResponse(CourseProgressView view);
    CourseProgressSummaryResponse toResponse(CourseProgressSummaryView view);
    CoursesProgressSummaryResponse toResponse(CoursesProgressSummaryView view);
    LearningItemProgressResponse toResponse(LearningItemProgressView view);
    TrackingVideoLessonCommand toCommand(TrackingVideoLessonRequest request);
    UpdateCourseProgressCommand toCommand(UpdateCourseProgressRequest request);
    UpdateLearningItemCommand toCommand(UpdateLearningItemRequest request);
    VideoLessonTrackingHistoryResponse toResponse(VideoLessonTrackingHistoryView view);
}
