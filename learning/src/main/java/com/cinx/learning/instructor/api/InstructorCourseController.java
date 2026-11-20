package com.cinx.learning.instructor.api;

import com.cinx.learning.instructor.api.mapper.InstructorApiMapper;

import com.cinx.common.dto.ApiResponse;
import com.cinx.common.dto.PaginatedApiResponse;
import com.cinx.common.mapper.PaginationWrapper;
import com.cinx.common.utils.AuthenticationUtil;
import com.cinx.learning.shared.domain.CourseMembershipRole;
import com.cinx.learning.instructor.domain.CourseLifecycle;
import com.cinx.learning.instructor.api.dto.CourseLearnersResponse;
import com.cinx.learning.instructor.api.dto.InstructorCourseRow;
import com.cinx.learning.instructor.api.dto.LessonReachResponse;
import com.cinx.learning.instructor.api.dto.QuizSummary;
import com.cinx.learning.instructor.application.IInstructorCourseService;
import com.cinx.learning.instructor.application.IInstructorCourseService.CourseQuery;
import com.cinx.learning.instructor.application.IInstructorCourseService.CourseSort;
import com.cinx.learning.instructor.application.IInstructorCourseService.LearnerFilter;
import com.cinx.learning.instructor.application.IInstructorCourseService.LearnerQuery;
import com.cinx.learning.instructor.application.IInstructorCourseService.LearnerSort;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

/** The instructor's Khoá học pages (api-contract-courses.md §1, §3–§5). */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/learning/instructor/courses")
public class InstructorCourseController {
    private final IInstructorCourseService courseService;
    private final InstructorApiMapper instructorApiMapper;

    @Operation(summary = "My courses (owned or co-taught) with role, lifecycle and numbers", security = @SecurityRequirement(name = "bearer-jwt"))
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_learning.course.update.own')")
    public ResponseEntity<PaginatedApiResponse<InstructorCourseRow>> courses(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "30") int size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Set<CourseLifecycle> lifecycle,
            @RequestParam(required = false) Set<CourseMembershipRole> role,
            @RequestParam(required = false) String categoryId,
            @RequestParam(defaultValue = "date") String sort,
            @RequestParam(defaultValue = "desc") String dir) {
        CourseQuery query = new CourseQuery(q, lifecycle, role, categoryId, enumOf(CourseSort.class, sort), "asc".equalsIgnoreCase(dir), page, size);
        return ResponseEntity.ok(PaginationWrapper.wrap(courseService.courses(AuthenticationUtil.extractUserId(), query).map(instructorApiMapper::toResponse)));
    }

    @Operation(summary = "One of my courses as a list row", security = @SecurityRequirement(name = "bearer-jwt"))
    @GetMapping("/{courseId}")
    @PreAuthorize("hasAuthority('PERM_learning.course.update.own')")
    public ResponseEntity<ApiResponse<InstructorCourseRow>> course(@PathVariable String courseId) {
        return ResponseEntity.ok(ApiResponse.success("", instructorApiMapper.toResponse(courseService.course(AuthenticationUtil.extractUserId(), courseId))));
    }

    @Operation(summary = "A course's learners, paged, with segment counts (no email)", security = @SecurityRequirement(name = "bearer-jwt"))
    @GetMapping("/{courseId}/learners")
    @PreAuthorize("hasAuthority('PERM_learning.submission.grade.own')")
    public ResponseEntity<ApiResponse<CourseLearnersResponse>> learners(
            @PathVariable String courseId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "all") String filter,
            @RequestParam(defaultValue = "joined") String sort,
            @RequestParam(defaultValue = "desc") String dir) {
        LearnerQuery query = new LearnerQuery(q, enumOf(LearnerFilter.class, filter),
                enumOf(LearnerSort.class, "lastActivity".equalsIgnoreCase(sort) ? "LAST_ACTIVITY" : sort),
                "asc".equalsIgnoreCase(dir), page, size);
        return ResponseEntity.ok(ApiResponse.success("", instructorApiMapper.toResponse(courseService.learners(AuthenticationUtil.extractUserId(), courseId, query))));
    }

    @Operation(summary = "How many learners completed each lesson, in course order", security = @SecurityRequirement(name = "bearer-jwt"))
    @GetMapping("/{courseId}/lesson-reach")
    @PreAuthorize("hasAuthority('PERM_learning.submission.grade.own')")
    public ResponseEntity<ApiResponse<LessonReachResponse>> lessonReach(@PathVariable String courseId) {
        return ResponseEntity.ok(ApiResponse.success("", instructorApiMapper.toResponse(courseService.lessonReach(AuthenticationUtil.extractUserId(), courseId))));
    }

    @Operation(summary = "A course's quizzes: attempts, average score, pass rate, essays waiting", security = @SecurityRequirement(name = "bearer-jwt"))
    @GetMapping("/{courseId}/quizzes")
    @PreAuthorize("hasAuthority('PERM_learning.submission.grade.own')")
    public ResponseEntity<ApiResponse<List<QuizSummary>>> quizzes(@PathVariable String courseId) {
        return ResponseEntity.ok(ApiResponse.success("", courseService.quizzes(AuthenticationUtil.extractUserId(), courseId).stream().map(instructorApiMapper::toResponse).toList()));
    }

    /** Lower-case query values map onto the enums; an unknown one is a 400 through the shared handler. */
    private static <E extends Enum<E>> E enumOf(Class<E> type, String value) {
        try {
            return Enum.valueOf(type, value.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new com.cinx.common.exception.BadRequestException(com.cinx.common.exception.ErrorCode.INVALID_SORT,
                    "Unknown value '" + value + "' for " + type.getSimpleName());
        }
    }
}
