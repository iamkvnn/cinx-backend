package com.cinx.learning.community.api;

import com.cinx.learning.community.api.mapper.CommunityApiMapper;

import com.cinx.common.dto.ApiResponse;
import com.cinx.common.dto.PaginatedApiResponse;
import com.cinx.common.mapper.PaginationWrapper;
import com.cinx.learning.community.api.dto.response.AdminReportResponse;
import com.cinx.learning.community.domain.ReportType;
import com.cinx.learning.community.application.IAdminReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/community/reports")
@RequiredArgsConstructor
public class AdminReportController {
    private final IAdminReportService adminReportService;
    private final CommunityApiMapper communityApiMapper;

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_learning.community.moderate.any') and @cinxMfaVerifier.requireSatisfied(authentication)")
    @Operation(security = @SecurityRequirement(name = "bearer-jwt"))
    public ResponseEntity<PaginatedApiResponse<AdminReportResponse>> getReports(
            @RequestParam(required = false) ReportType type,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String sort) {
        return ResponseEntity.ok(PaginationWrapper.wrap(adminReportService.getReports(type, page, size, query, sort).map(communityApiMapper::toResponse)));
    }

    @DeleteMapping("/{reportId}/dismiss")
    @PreAuthorize("hasAuthority('PERM_learning.community.moderate.any') and @cinxMfaVerifier.requireSatisfied(authentication)")
    @Operation(security = @SecurityRequirement(name = "bearer-jwt"))
    public ResponseEntity<ApiResponse<Void>> dismissReport(@PathVariable String reportId) {
        adminReportService.dismissReport(reportId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Report dismissed", null));
    }

    @DeleteMapping("/{reportId}/content")
    @PreAuthorize("hasAuthority('PERM_learning.community.moderate.any') and @cinxMfaVerifier.requireSatisfied(authentication)")
    @Operation(security = @SecurityRequirement(name = "bearer-jwt"))
    public ResponseEntity<ApiResponse<Void>> deleteReportedContent(@PathVariable String reportId) {
        adminReportService.deleteReportedContent(reportId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Reported content deleted", null));
    }
}
