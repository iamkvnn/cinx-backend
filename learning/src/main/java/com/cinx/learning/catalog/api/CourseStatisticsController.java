package com.cinx.learning.catalog.api;

import com.cinx.learning.catalog.api.mapper.CatalogApiMapper;

import com.cinx.common.dto.ApiResponse;
import com.cinx.common.utils.AuthenticationUtil;
import com.cinx.learning.catalog.api.dto.response.AdminCourseStatisticsOverviewResponse;
import com.cinx.learning.catalog.api.dto.response.InstructorCourseStatisticsOverviewResponse;
import com.cinx.learning.catalog.application.ICourseStatisticsService;
import com.cinx.learning.catalog.domain.StatisticsGroupBy;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
public class CourseStatisticsController {
    private final ICourseStatisticsService courseStatisticsService;
    private final CatalogApiMapper catalogApiMapper;

    @GetMapping("/api/v1/courses/admin/statistics/overview")
    @PreAuthorize("hasAuthority('PERM_learning.analytics.read.any')")
    @Operation(summary = "Get admin course statistics overview", security = @SecurityRequirement(name = "bearer-jwt"))
    public ResponseEntity<ApiResponse<AdminCourseStatisticsOverviewResponse>> getAdminOverview(
            @RequestParam(required = false) StatisticsGroupBy groupBy,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Admin course statistics fetched successfully",
                catalogApiMapper.toResponse(courseStatisticsService.getAdminOverview(groupBy, startDate, endDate))
        ));
    }

    @GetMapping("/api/v1/courses/mine/statistics/overview")
    @Operation(summary = "Get my course statistics overview", security = @SecurityRequirement(name = "bearer-jwt"))
    public ResponseEntity<ApiResponse<InstructorCourseStatisticsOverviewResponse>> getInstructorOverview(
            @RequestParam(required = false) StatisticsGroupBy groupBy,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        String instructorId = AuthenticationUtil.extractUserId();
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Instructor course statistics fetched successfully",
                catalogApiMapper.toResponse(courseStatisticsService.getInstructorOverview(instructorId, groupBy, startDate, endDate))
        ));
    }
}

