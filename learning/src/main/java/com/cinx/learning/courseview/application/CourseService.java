package com.cinx.learning.courseview.application;

import com.cinx.learning.courseview.application.query.CourseView;
import com.cinx.learning.courseview.application.query.LessonView;
import com.cinx.learning.courseview.application.query.QuizLessonView;
import com.cinx.learning.courseview.application.query.VideoLessonView;

import com.cinx.common.dto.ApiResponse;
import com.cinx.common.utils.AuthenticationUtil;
import com.cinx.learning.catalog.application.ICourseService;
import com.cinx.learning.catalog.application.ILessonService;
import com.cinx.learning.catalog.application.IQuizService;
import com.cinx.learning.catalog.application.IVideoService;
import com.cinx.learning.catalog.application.IVideoQuestionService;
import com.cinx.learning.courseview.application.mapper.LearningCatalogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service("learningCourseFacade")
@RequiredArgsConstructor
public class CourseService {
    private final ICourseService courses;
    private final ILessonService lessons;
    private final IQuizService quizzes;
    private final IVideoService videos;
    private final IVideoQuestionService videoQuestions;
    private final LearningCatalogMapper mapper;

    public ApiResponse<CourseView> getCourseById(String id) {
        return ApiResponse.success("Course fetched", mapper.toLearning(
                courses.getReadableCourseById(AuthenticationUtil.extractUserId(), id)));
    }

    public ApiResponse<List<String>> getCourseLessonIdsByCourseId(String id) {
        return ApiResponse.success("Lessons fetched", lessons.getEnrolledLessonIdsByCourseId(id));
    }

    public ApiResponse<List<String>> getCourseIdsByInstructor(String instructorId) {
        return ApiResponse.success("Course ids fetched", courses.getCourseIdsByInstructor(instructorId));
    }

    public ApiResponse<LessonView> getEnrolledLessonById(String courseId, String lessonId) {
        return ApiResponse.success("Lesson fetched",
                mapper.toLearning(lessons.getUserReadableLessonByCourseIdAndLessonId(
                        AuthenticationUtil.extractUserId(), courseId, lessonId)));
    }

    public ApiResponse<Boolean> isLessonInstructor(String lessonId, String userId) {
        return ApiResponse.success("Instructor access checked", lessons.isLessonInstructor(lessonId, userId));
    }

    public ApiResponse<QuizLessonView> getQuizLessonById(String courseId, String lessonId) {
        return ApiResponse.success("Quiz fetched", mapper.toLearning(
                quizzes.getQuizByLessonId(AuthenticationUtil.extractUserId(), courseId, lessonId)));
    }

    public ApiResponse<VideoLessonView> getVideoLessonById(String courseId, String lessonId) {
        return ApiResponse.success("Video fetched", mapper.toLearning(
                videos.getReadableVideoByLessonId(AuthenticationUtil.extractUserId(), courseId, lessonId)));
    }

    public ApiResponse<Boolean> checkVideoQuestionAnswer(String id, String answer) {
        return ApiResponse.success("Answer checked", videoQuestions.checkAnswer(id, answer));
    }
}
