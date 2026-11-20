package com.cinx.learning.catalog.api;

import com.cinx.learning.catalog.api.mapper.CatalogApiMapper;

import com.cinx.learning.catalog.api.dto.response.CourseCurriculumResponse;
import com.cinx.learning.catalog.api.dto.response.CourseResponse;
import com.cinx.learning.catalog.api.dto.response.LessonResponse;
import com.cinx.learning.catalog.api.dto.response.QuizLessonResponse;
import com.cinx.learning.catalog.api.dto.response.VideoLessonResponse;

import com.cinx.common.dto.ApiResponse;
import com.cinx.learning.catalog.application.ICourseService;
import com.cinx.learning.catalog.application.ICourseCollaborationService;
import com.cinx.learning.catalog.application.ICurriculumService;
import com.cinx.learning.catalog.application.ILessonService;
import com.cinx.learning.catalog.application.IQuizService;
import com.cinx.learning.catalog.application.IVideoService;
import com.cinx.learning.catalog.application.IVideoQuestionService;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Internal API â€” called only by other services via Feign (service-to-service).
 * Not exposed externally; blocked at the gateway layer (/internal/** â†’ denyAll).
 */
@Hidden
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InternalCourseController {

    private final ICourseService courseService;
    private final ICurriculumService curriculumService;
    private final ILessonService lessonService;
    private final IQuizService quizService;
    private final IVideoService videoService;
    private final IVideoQuestionService videoQuestionService;
    private final ICourseCollaborationService collaborationService;
    private final CatalogApiMapper catalogApiMapper;

    @GetMapping("/courses/{id}")
    @PreAuthorize("hasAuthority('PERM_learning.internal.course.read') "
            + "and @cinxServiceClient.is(authentication, 'cinx-commerce-service', 'cinx-notification-service', 'cinx-recommendation-service')")
    public ApiResponse<CourseResponse> getReadableCourseById(
            @PathVariable String id
    ) {
        // a service needs the course whatever its price; the reader-access rule would hide every paid course
        return new ApiResponse<>(true, "Course fetched successfully", catalogApiMapper.toResponse(courseService.getCourseForService(id)));
    }

    @GetMapping("/courses/{id}/lessons")
    @PreAuthorize("hasAuthority('PERM_learning.internal.course.read') "
            + "and @cinxServiceClient.is(authentication, 'cinx-commerce-service', 'cinx-notification-service', 'cinx-recommendation-service')")
    public ApiResponse<List<String>> getCourseLessonIdsByCourseId(@PathVariable String id) {
        return new ApiResponse<>(true, "Course fetched successfully", lessonService.getEnrolledLessonIdsByCourseId(id));
    }

    @GetMapping("/courses/{courseId}/lessons/{lessonId}")
    @PreAuthorize("hasAuthority('PERM_learning.internal.course.read') "
            + "and @cinxServiceClient.is(authentication, 'cinx-commerce-service', 'cinx-notification-service', 'cinx-recommendation-service')")
    public ApiResponse<LessonResponse> getEnrolledLessonById(
            @PathVariable String courseId,
            @PathVariable String lessonId
    ) {
        return new ApiResponse<>(true, "Lesson fetched successfully", catalogApiMapper.toResponse(lessonService.getEnrolledLessonByCourseIdAndLessonId(courseId, lessonId)));
    }

    @GetMapping("/lessons/{lessonId}/instructor-access")
    @PreAuthorize("hasAuthority('PERM_learning.internal.course.read') "
            + "and @cinxServiceClient.is(authentication, 'cinx-commerce-service', 'cinx-notification-service', 'cinx-recommendation-service')")
    public ApiResponse<Boolean> isLessonInstructor(
            @PathVariable String lessonId,
            @RequestParam String userId
    ) {
        return new ApiResponse<>(true, "Instructor access checked successfully", lessonService.isLessonInstructor(lessonId, userId));
    }

    @GetMapping("/courses/{id}/curriculum")
    @PreAuthorize("hasAuthority('PERM_learning.internal.course.read') "
            + "and @cinxServiceClient.is(authentication, 'cinx-commerce-service', 'cinx-notification-service', 'cinx-recommendation-service')")
    public ApiResponse<CourseCurriculumResponse> getCourseCurriculum(
            @PathVariable String id
    ) {
        return new ApiResponse<>(true, "Course curriculum fetched successfully", catalogApiMapper.toResponse(curriculumService.getReadableCurriculum(null, id)));
    }

    @GetMapping("/courses/ids")
    @PreAuthorize("hasAuthority('PERM_learning.internal.course.read') "
            + "and @cinxServiceClient.is(authentication, 'cinx-commerce-service', 'cinx-notification-service', 'cinx-recommendation-service')")
    public ApiResponse<List<CourseResponse>> getCoursesByIds(
            @RequestParam List<String> ids
    ) {
        return new ApiResponse<>(true, "Courses fetched successfully", courseService.getCoursesForService(ids).stream().map(catalogApiMapper::toResponse).toList());
    }

    @GetMapping("/courses/search-ids")
    @PreAuthorize("hasAuthority('PERM_learning.internal.course.read') "
            + "and @cinxServiceClient.is(authentication, 'cinx-commerce-service', 'cinx-notification-service', 'cinx-recommendation-service')")
    public ApiResponse<List<String>> searchReadableCourseIds(
            @RequestParam List<String> ids,
            @RequestParam String query
    ) {
        return new ApiResponse<>(true, "Course ids fetched successfully", courseService.searchReadableCourseIds(ids, query));
    }

    @GetMapping("/courses/instructors/{instructorId}/ids")
    @PreAuthorize("hasAuthority('PERM_learning.internal.course.read') "
            + "and @cinxServiceClient.is(authentication, 'cinx-commerce-service', 'cinx-notification-service', 'cinx-recommendation-service')")
    public ApiResponse<List<String>> getCourseIdsByInstructor(@PathVariable String instructorId) {
        return new ApiResponse<>(true, "Instructor course ids fetched successfully", courseService.getCourseIdsByInstructor(instructorId));
    }

    @GetMapping("/courses/{id}/instructor-ids")
    @PreAuthorize("hasAuthority('PERM_learning.internal.course.read') "
            + "and @cinxServiceClient.is(authentication, 'cinx-notification-service')")
    public ApiResponse<List<String>> getActiveInstructorIds(@PathVariable String id) {
        return new ApiResponse<>(true, "Course instructor ids fetched successfully",
                collaborationService.activeInstructorIds(id));
    }

    @PostMapping("/courses/{id}/update-rating")
    @PreAuthorize("hasAuthority('PERM_learning.internal.course.read') "
            + "and @cinxServiceClient.is(authentication, 'cinx-commerce-service', 'cinx-notification-service', 'cinx-recommendation-service')")
    public ApiResponse<Void> updateCourseRating(@PathVariable String id, @RequestParam(required = false) Double rating) {
        courseService.updateCourseRating(id, rating);
        return new ApiResponse<>(true, "Rating updated successfully", null);
    }

    @PostMapping("/courses/{id}/increase-enrollment")
    @PreAuthorize("hasAuthority('PERM_learning.internal.course.read') "
            + "and @cinxServiceClient.is(authentication, 'cinx-commerce-service', 'cinx-notification-service', 'cinx-recommendation-service')")
    public ApiResponse<Void> increaseEnrollmentCount(@PathVariable String id) {
        courseService.increaseEnrollmentCount(id);
        return new ApiResponse<>(true, "Enrollment count increased successfully", null);
    }

    @GetMapping("/courses/{courseId}/lessons/{lessonId}/quizzes")
    @PreAuthorize("hasAuthority('PERM_learning.internal.course.read') "
            + "and @cinxServiceClient.is(authentication, 'cinx-commerce-service', 'cinx-notification-service', 'cinx-recommendation-service')")
    public ApiResponse<QuizLessonResponse> getQuizLessonById(
            @PathVariable String courseId,
            @PathVariable String lessonId
    ) {
        return new ApiResponse<>(true, "Quiz lesson fetched successfully", catalogApiMapper.toResponse(quizService.getQuizByLessonId(null, courseId, lessonId)));
    }

    @GetMapping("/courses/{courseId}/lessons/{lessonId}/videos")
    @PreAuthorize("hasAuthority('PERM_learning.internal.course.read') "
            + "and @cinxServiceClient.is(authentication, 'cinx-commerce-service', 'cinx-notification-service', 'cinx-recommendation-service')")
    public ApiResponse<VideoLessonResponse> getVideoLessonById(
            @PathVariable String courseId,
            @PathVariable String lessonId
    ) {
        return new ApiResponse<>(true, "Video lesson fetched successfully", catalogApiMapper.toResponse(videoService.getReadableVideoByLessonId(null, courseId, lessonId)));
    }

    @GetMapping("/video-questions/{id}/check-answer")
    @PreAuthorize("hasAuthority('PERM_learning.internal.course.read') "
            + "and @cinxServiceClient.is(authentication, 'cinx-commerce-service', 'cinx-notification-service', 'cinx-recommendation-service')")
    public ApiResponse<Boolean> checkVideoQuestionAnswer(@PathVariable String id, @RequestParam String answer) {
        return new ApiResponse<>(true, "Answer checked successfully", videoQuestionService.checkAnswer(id, answer));
    }
}

