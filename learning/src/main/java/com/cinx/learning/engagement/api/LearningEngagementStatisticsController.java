package com.cinx.learning.engagement.api;

import com.cinx.learning.engagement.api.mapper.EngagementApiMapper;

import com.cinx.common.dto.ApiResponse;
import com.cinx.common.utils.AuthenticationUtil;
import com.cinx.learning.engagement.api.dto.response.CourseEngagementOverviewResponse;
import com.cinx.learning.engagement.domain.LearningActivityGroupBy;
import com.cinx.learning.engagement.application.ILearningEngagementStatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/learning")
@RequiredArgsConstructor
public class LearningEngagementStatisticsController {
    private final ILearningEngagementStatisticsService statisticsService;
    private final EngagementApiMapper engagementApiMapper;

    @GetMapping("/courses/{courseId}/engagement/overview")
    @PreAuthorize("hasAuthority('PERM_learning.analytics.read.own') or hasAuthority('PERM_learning.analytics.read.any')")
    @Operation(summary = "Get instructor course engagement overview", security = @SecurityRequirement(name = "bearer-jwt"))
    public ResponseEntity<ApiResponse<CourseEngagementOverviewResponse>> getInstructorCourseEngagement(
            @PathVariable String courseId,
            @RequestParam(required = false) LearningActivityGroupBy groupBy,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Authentication authentication
    ) {
        String currentUserId = AuthenticationUtil.extractUserId();
        boolean readAny = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("PERM_learning.analytics.read.any"));
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Course engagement overview fetched successfully",
                readAny
                        ? engagementApiMapper.toResponse(statisticsService.getAdminCourseEngagement(courseId, groupBy, startDate, endDate))
                        : engagementApiMapper.toResponse(statisticsService.getInstructorCourseEngagement(currentUserId, courseId, groupBy, startDate, endDate))
        ));
    }

    @GetMapping("/admin/courses/{courseId}/engagement/overview")
    @PreAuthorize("hasAuthority('PERM_learning.analytics.read.any')")
    @Operation(summary = "Get admin course engagement overview", security = @SecurityRequirement(name = "bearer-jwt"))
    public ResponseEntity<ApiResponse<CourseEngagementOverviewResponse>> getAdminCourseEngagement(
            @PathVariable String courseId,
            @RequestParam(required = false) LearningActivityGroupBy groupBy,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Course engagement overview fetched successfully",
                engagementApiMapper.toResponse(statisticsService.getAdminCourseEngagement(courseId, groupBy, startDate, endDate))
        ));
    }
}
