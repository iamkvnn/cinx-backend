package com.cinx.learning.catalog.api;

import com.cinx.learning.catalog.api.mapper.CatalogApiMapper;

import com.cinx.common.dto.ApiResponse;
import com.cinx.learning.catalog.api.dto.response.InstructorCourseSummaryResponse;
import com.cinx.learning.catalog.application.ICourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/courses/admin/instructors")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('PERM_learning.analytics.read.any')")
public class AdminInstructorController {
    private final ICourseService courseService;
    private final CatalogApiMapper catalogApiMapper;

    @Operation(security = @SecurityRequirement(name = "bearer-jwt"))
    @GetMapping("/{instructorId}/course-summary")
    public ResponseEntity<ApiResponse<InstructorCourseSummaryResponse>> getCourseSummary(@PathVariable String instructorId) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Instructor course summary fetched successfully",
                catalogApiMapper.toResponse(courseService.getInstructorCourseSummary(instructorId))
        ));
    }
}

