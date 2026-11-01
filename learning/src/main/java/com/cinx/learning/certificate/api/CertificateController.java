package com.cinx.learning.certificate.api;

import com.cinx.common.paging.PageResult;

import com.cinx.learning.certificate.api.mapper.CertificateApiMapper;

import com.cinx.common.dto.ApiResponse;
import com.cinx.common.dto.PaginatedApiResponse;
import com.cinx.common.utils.AuthenticationUtil;
import com.cinx.learning.shared.domain.CertificateStatus;
import com.cinx.learning.certificate.api.dto.request.ApproveCertificatesRequest;
import com.cinx.learning.certificate.api.dto.request.RejectCertificateRequest;
import com.cinx.learning.certificate.api.dto.response.CertificateConditionsResponse;
import com.cinx.learning.certificate.api.dto.response.CertificateRequestResponse;
import com.cinx.learning.certificate.application.ICertificateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/certificates")
public class CertificateController {
    
    private final ICertificateService certificateService;
    private final CertificateApiMapper certificateApiMapper;

    @Operation(summary = "Apply for course certificate", security = @SecurityRequirement(name = "bearer-jwt"))
    @PostMapping("/apply/{courseId}")
    public ResponseEntity<ApiResponse<CertificateRequestResponse>> applyForCertificate(@PathVariable String courseId) {
        String userId = AuthenticationUtil.extractUserId();
        return ResponseEntity.ok(new ApiResponse<>(true, "Requested successfully", certificateApiMapper.toResponse(certificateService.applyForCertificate(userId, courseId))));
    }

    @Operation(summary = "Get user certificate by course", security = @SecurityRequirement(name = "bearer-jwt"))
    @GetMapping("/my-certificate/{courseId}")
    public ResponseEntity<ApiResponse<CertificateRequestResponse>> getMyCertificate(@PathVariable String courseId) {
        String userId = AuthenticationUtil.extractUserId();
        return ResponseEntity.ok(new ApiResponse<>(true, "Success", certificateApiMapper.toResponse(certificateService.getCertificate(userId, courseId))));
    }

    @Operation(summary = "Get all user certificates", security = @SecurityRequirement(name = "bearer-jwt"))
    @GetMapping("/my-certificates")
    public ResponseEntity<ApiResponse<java.util.List<CertificateRequestResponse>>> getMyCertificates() {
        String userId = AuthenticationUtil.extractUserId();
        return ResponseEntity.ok(new ApiResponse<>(true, "Success", certificateService.getMyCertificates(userId).stream().map(certificateApiMapper::toResponse).toList()));
    }

    @Operation(summary = "Get certificate requests by course ID (For Instructor)", security = @SecurityRequirement(name = "bearer-jwt"))
    @GetMapping("/requests/{courseId}")
    @PreAuthorize("hasAuthority('PERM_learning.submission.grade.own')")
    public ResponseEntity<PaginatedApiResponse<CertificateRequestResponse>> getRequestsByCourse(
            @PathVariable String courseId,
            @RequestParam(required = false) CertificateStatus status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String sort
    ) {
        String currentUserId = AuthenticationUtil.extractUserId();
        PageResult<CertificateRequestResponse> result = certificateService.getRequestsByCourse(currentUserId, courseId, status, page, size, query, sort).map(certificateApiMapper::toResponse);
        return ResponseEntity.ok(new PaginatedApiResponse<>(
                true, "Success",
                result.content(),
                new com.cinx.common.dto.PaginatedMetadata(result.page(), size, result.totalElements(), result.totalPages())
        ));
    }

    @Operation(summary = "Get all certificate requests", security = @SecurityRequirement(name = "bearer-jwt"))
    @GetMapping("/requests")
    @PreAuthorize("hasAuthority('PERM_learning.submission.grade.own')")
    public ResponseEntity<PaginatedApiResponse<CertificateRequestResponse>> getAllRequests(
            @RequestParam(required = false) CertificateStatus status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String sort
    ) {
        String currentUserId = AuthenticationUtil.extractUserId();
        PageResult<CertificateRequestResponse> result = certificateService.getAllRequests(currentUserId, status, page, size, query, sort).map(certificateApiMapper::toResponse);
        return ResponseEntity.ok(new PaginatedApiResponse<>(
                true, "Success",
                result.content(),
                new com.cinx.common.dto.PaginatedMetadata(result.page(), size, result.totalElements(), result.totalPages())
        ));
    }

    @Operation(summary = "Approve certificate request (For Instructor)", security = @SecurityRequirement(name = "bearer-jwt"))
    @PutMapping("/requests/{requestId}/approve")
    @PreAuthorize("hasAuthority('PERM_learning.submission.grade.own')")
    public ResponseEntity<ApiResponse<CertificateRequestResponse>> approveCertificate(@PathVariable String requestId) {
        String currentUserId = AuthenticationUtil.extractUserId();
        return ResponseEntity.ok(new ApiResponse<>(true, "Approved successfully", certificateApiMapper.toResponse(certificateService.approveCertificate(currentUserId, requestId))));
    }

    @Operation(summary = "Reject certificate request (For Instructor)", security = @SecurityRequirement(name = "bearer-jwt"))
    @PutMapping("/requests/{requestId}/reject")
    @PreAuthorize("hasAuthority('PERM_learning.submission.grade.own')")
    public ResponseEntity<ApiResponse<CertificateRequestResponse>> rejectCertificate(@PathVariable String requestId,
                                                                                     @Valid @RequestBody(required = false) RejectCertificateRequest request) {
        String currentUserId = AuthenticationUtil.extractUserId();
        // a missing body reaches the service as no reason, which refuses it with REJECTION_REASON_REQUIRED
        String reason = request == null ? null : request.reason();
        return ResponseEntity.ok(new ApiResponse<>(true, "Rejected successfully", certificateApiMapper.toResponse(certificateService.rejectCertificate(currentUserId, requestId, reason))));
    }

    @Operation(summary = "Approve several certificate requests (For Instructor)", security = @SecurityRequirement(name = "bearer-jwt"))
    @PostMapping("/requests/approve-batch")
    @PreAuthorize("hasAuthority('PERM_learning.submission.grade.own')")
    public ResponseEntity<ApiResponse<CertificateConditionsResponse.BatchResult>> approveCertificates(
            @Valid @RequestBody ApproveCertificatesRequest request) {
        String currentUserId = AuthenticationUtil.extractUserId();
        return ResponseEntity.ok(ApiResponse.success("Approved", certificateApiMapper.toResponse(certificateService.approveCertificates(currentUserId, request.requestIds()))));
    }
}
