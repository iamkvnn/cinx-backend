package com.cinx.learning.certificate.api;

import com.cinx.learning.certificate.api.mapper.CertificateApiMapper;

import com.cinx.common.dto.ApiResponse;
import com.cinx.common.utils.AuthenticationUtil;
import com.cinx.learning.certificate.api.dto.request.CertificateConditionsRequest;
import com.cinx.learning.certificate.api.dto.response.CertificateConditionsResponse;
import com.cinx.learning.certificate.application.ICertificateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** A course's certificate conditions, set by its instructors. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/courses/{courseId}/certificate-conditions")
@PreAuthorize("hasAuthority('PERM_learning.submission.grade.own')")
public class CertificateConditionsController {
    private final ICertificateService certificateService;
    private final CertificateApiMapper certificateApiMapper;

    @Operation(summary = "Get a course's certificate conditions (defaults when none were set)", security = @SecurityRequirement(name = "bearer-jwt"))
    @GetMapping
    public ResponseEntity<ApiResponse<CertificateConditionsResponse>> get(@PathVariable String courseId) {
        String userId = AuthenticationUtil.extractUserId();
        return ResponseEntity.ok(ApiResponse.success("", certificateApiMapper.toResponse(certificateService.getConditions(userId, courseId))));
    }

    @Operation(summary = "Save a course's certificate conditions and re-check pending requests", security = @SecurityRequirement(name = "bearer-jwt"))
    @PutMapping
    public ResponseEntity<ApiResponse<CertificateConditionsResponse.Saved>> save(
            @PathVariable String courseId,
            @Valid @RequestBody CertificateConditionsRequest request) {
        String userId = AuthenticationUtil.extractUserId();
        return ResponseEntity.ok(ApiResponse.success("Conditions saved", certificateApiMapper.toResponse(certificateService.saveConditions(userId, courseId, certificateApiMapper.toCommand(request)))));
    }
}
