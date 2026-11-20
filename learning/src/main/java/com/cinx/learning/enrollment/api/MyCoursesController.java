package com.cinx.learning.enrollment.api;

import com.cinx.learning.enrollment.api.mapper.EnrollmentApiMapper;

import com.cinx.common.dto.ApiResponse;
import com.cinx.common.utils.AuthenticationUtil;
import com.cinx.learning.enrollment.api.dto.response.MyCourseResponse;
import com.cinx.learning.enrollment.application.IMyCoursesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/learning/my-courses")
@RequiredArgsConstructor
public class MyCoursesController {
    private final IMyCoursesService myCoursesService;
    private final EnrollmentApiMapper enrollmentApiMapper;

    @Operation(summary = "My courses with progress, the lesson to resume and the certificate request, most recently studied first",
            security = @SecurityRequirement(name = "bearer-jwt"))
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_learning.progress.read.self')")
    public ResponseEntity<ApiResponse<List<MyCourseResponse>>> myCourses() {
        return ResponseEntity.ok(new ApiResponse<>(true, "My courses fetched successfully",
                myCoursesService.myCourses(AuthenticationUtil.extractUserId()).stream().map(enrollmentApiMapper::toResponse).toList()));
    }
}
