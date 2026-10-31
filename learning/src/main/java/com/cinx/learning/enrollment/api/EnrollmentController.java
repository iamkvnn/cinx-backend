package com.cinx.learning.enrollment.api;

import com.cinx.learning.enrollment.api.mapper.EnrollmentApiMapper;

import com.cinx.common.dto.ApiResponse;
import com.cinx.common.utils.AuthenticationUtil;
import com.cinx.learning.enrollment.api.dto.response.EnrollmentResponse;
import com.cinx.learning.enrollment.application.IEnrollmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/enrollments")
@RequiredArgsConstructor
public class EnrollmentController {
    private final IEnrollmentService enrollmentService;
    private final EnrollmentApiMapper enrollmentApiMapper;

    @GetMapping
    @Operation(security = @SecurityRequirement(name = "bearer-jwt"))
    public ResponseEntity<ApiResponse<List<EnrollmentResponse>>> getMyEnrollments() {
        List<EnrollmentResponse> enrollments = enrollmentService.findMine(AuthenticationUtil.extractUserId()).stream().map(enrollmentApiMapper::toResponse).toList();
        return ResponseEntity.ok(ApiResponse.success("Enrollments fetched", enrollments));
    }
}
