package com.cinx.learning.engagement.api;

import com.cinx.learning.progress.api.mapper.ProgressApiMapper;

import com.cinx.learning.engagement.api.mapper.EngagementApiMapper;

import com.cinx.common.dto.ApiResponse;
import com.cinx.learning.progress.api.dto.response.CourseProgressResponse;
import com.cinx.learning.progress.api.dto.response.CoursesProgressSummaryResponse;
import com.cinx.learning.engagement.api.dto.response.LearningActivityByTimeResponse;
import com.cinx.learning.engagement.api.dto.response.UserLearningSummaryResponse;
import com.cinx.learning.engagement.application.ILearningActivityService;
import com.cinx.learning.engagement.domain.LearningActivityGroupBy;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/learning/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('PERM_learning.analytics.read.any')")
public class AdminLearningController {
    private final ILearningActivityService learningActivityService;
    private final EngagementApiMapper engagementApiMapper;
    private final ProgressApiMapper progressApiMapper;

    @Operation(security = @SecurityRequirement(name = "bearer-jwt"))
    @GetMapping("/users/{userId}/summary")
    public ResponseEntity<ApiResponse<UserLearningSummaryResponse>> getUserSummary(@PathVariable String userId) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "User learning summary fetched successfully",
                engagementApiMapper.toResponse(learningActivityService.getUserLearningSummary(userId))
        ));
    }

    @Operation(security = @SecurityRequirement(name = "bearer-jwt"))
    @GetMapping("/users/{userId}/course-progress")
    public ResponseEntity<ApiResponse<List<CourseProgressResponse>>> getUserCourseProgress(
            @PathVariable String userId,
            @RequestParam List<String> courseIds
    ) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "User course progress fetched successfully",
                learningActivityService.getUserCourseProgress(userId, courseIds).stream().map(progressApiMapper::toResponse).toList()
        ));
    }

    @Operation(security = @SecurityRequirement(name = "bearer-jwt"))
    @GetMapping("/users/{userId}/activity/series")
    public ResponseEntity<ApiResponse<List<LearningActivityByTimeResponse>>> getUserActivitySeries(
            @PathVariable String userId,
            @RequestParam(required = false) LearningActivityGroupBy groupBy,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "User learning activity fetched successfully",
                learningActivityService.getUserActivitySeries(userId, groupBy, startDate, endDate).stream().map(engagementApiMapper::toResponse).toList()
        ));
    }

    @Operation(security = @SecurityRequirement(name = "bearer-jwt"))
    @GetMapping("/courses/progress-summary")
    public ResponseEntity<ApiResponse<CoursesProgressSummaryResponse>> getCoursesProgressSummary(@RequestParam List<String> courseIds) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Courses progress summary fetched successfully",
                progressApiMapper.toResponse(learningActivityService.getCoursesProgressSummary(courseIds))
        ));
    }
}
