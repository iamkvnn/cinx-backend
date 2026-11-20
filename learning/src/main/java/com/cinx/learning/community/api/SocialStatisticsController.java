package com.cinx.learning.community.api;

import com.cinx.learning.community.api.mapper.CommunityApiMapper;

import com.cinx.common.dto.ApiResponse;
import com.cinx.common.utils.AuthenticationUtil;
import com.cinx.learning.community.api.dto.response.CourseQnAStatisticsResponse;
import com.cinx.learning.community.api.dto.response.ReportStatisticsOverviewResponse;
import com.cinx.learning.community.api.dto.response.ReviewStatisticsResponse;
import com.cinx.learning.community.application.ISocialStatisticsService;
import com.cinx.learning.community.domain.StatisticsGroupBy;
import com.cinx.learning.shared.application.LearningAuthorizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
public class SocialStatisticsController {
    private final ISocialStatisticsService socialStatisticsService;
    private final LearningAuthorizationService authorizationService;
    private final CommunityApiMapper communityApiMapper;

    @GetMapping("/api/v1/community/reviews/statistics/courses/{courseId}")
    @Operation(summary = "Get course review statistics", security = @SecurityRequirement(name = "bearer-jwt"))
    public ResponseEntity<ApiResponse<ReviewStatisticsResponse>> getReviewStatistics(@PathVariable String courseId) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Review statistics fetched successfully",
                communityApiMapper.toResponse(socialStatisticsService.getReviewStatistics(courseId))
        ));
    }

    @GetMapping("/api/v1/community/qna/statistics/courses/{courseId}")
    @Operation(summary = "Get course QnA statistics", security = @SecurityRequirement(name = "bearer-jwt"))
    public ResponseEntity<ApiResponse<CourseQnAStatisticsResponse>> getCourseQnAStatistics(
            @PathVariable String courseId,
            @RequestParam(required = false) StatisticsGroupBy groupBy,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        // Question and answer counts of a course are the course team's business, not every signed-in user's.
        authorizationService.requireCourseInstructor(AuthenticationUtil.extractUserId(), courseId);
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Course QnA statistics fetched successfully",
                communityApiMapper.toResponse(socialStatisticsService.getCourseQnAStatistics(courseId, groupBy, startDate, endDate))
        ));
    }

    @GetMapping("/api/v1/community/reports/statistics/overview")
    @PreAuthorize("hasAuthority('PERM_learning.community.moderate.any') and @cinxMfaVerifier.requireSatisfied(authentication)")
    @Operation(summary = "Get report statistics overview", security = @SecurityRequirement(name = "bearer-jwt"))
    public ResponseEntity<ApiResponse<ReportStatisticsOverviewResponse>> getReportOverview(
            @RequestParam(required = false) StatisticsGroupBy groupBy,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Report statistics fetched successfully",
                communityApiMapper.toResponse(socialStatisticsService.getReportOverview(groupBy, startDate, endDate))
        ));
    }
}
