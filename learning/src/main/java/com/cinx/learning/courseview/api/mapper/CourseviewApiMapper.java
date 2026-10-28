package com.cinx.learning.courseview.api.mapper;

import com.cinx.learning.courseview.api.dto.response.CategoryResponse;
import com.cinx.learning.courseview.api.dto.response.CourseImageResponse;
import com.cinx.learning.courseview.api.dto.response.CourseResponse;
import com.cinx.learning.courseview.api.dto.response.InstructorResponse;
import com.cinx.learning.courseview.api.dto.response.LessonResponse;
import com.cinx.learning.courseview.api.dto.response.QuizLessonResponse;
import com.cinx.learning.courseview.api.dto.response.QuizOptionResponse;
import com.cinx.learning.courseview.api.dto.response.QuizQuestionResponse;
import com.cinx.learning.courseview.api.dto.response.SubtitleTrackResponse;
import com.cinx.learning.courseview.api.dto.response.UserDto;
import com.cinx.learning.courseview.api.dto.response.VideoLessonResponse;
import com.cinx.learning.courseview.application.query.CategoryView;
import com.cinx.learning.courseview.application.query.CourseImageView;
import com.cinx.learning.courseview.application.query.CourseView;
import com.cinx.learning.courseview.application.query.InstructorView;
import com.cinx.learning.courseview.application.query.LessonView;
import com.cinx.learning.courseview.application.query.QuizLessonView;
import com.cinx.learning.courseview.application.query.QuizOptionView;
import com.cinx.learning.courseview.application.query.QuizQuestionView;
import com.cinx.learning.courseview.application.query.SubtitleTrackView;
import com.cinx.learning.courseview.application.query.UserView;
import com.cinx.learning.courseview.application.query.VideoLessonView;
import org.mapstruct.Mapper;

/** Converts the api's transport types to the application's commands and the application's views to transport types. */
@Mapper(componentModel = "spring")
public interface CourseviewApiMapper {
    CategoryResponse toResponse(CategoryView view);
    CourseImageResponse toResponse(CourseImageView view);
    CourseResponse toResponse(CourseView view);
    InstructorResponse toResponse(InstructorView view);
    LessonResponse toResponse(LessonView view);
    QuizLessonResponse toResponse(QuizLessonView view);
    QuizOptionResponse toResponse(QuizOptionView view);
    QuizQuestionResponse toResponse(QuizQuestionView view);
    SubtitleTrackResponse toResponse(SubtitleTrackView view);
    UserDto toResponse(UserView view);
    VideoLessonResponse toResponse(VideoLessonView view);
}
