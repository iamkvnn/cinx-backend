package com.cinx.learning.community.api.internal;

import com.cinx.common.dto.PaginatedApiResponse;
import com.cinx.common.mapper.PaginationWrapper;
import com.cinx.learning.community.api.dto.response.AdminReportResponse;
import com.cinx.learning.community.api.mapper.CommunityApiMapper;
import com.cinx.learning.community.application.IAdminReportService;
import com.cinx.learning.community.domain.ReportType;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only report list for the admin copilot's triage. The assistant checks the admin's moderate.any and a fresh
 * MFA before it calls, and audits each call; nothing here can dismiss a report or delete content.
 */
@Hidden
@RestController
@RequestMapping("/internal/community/reports")
@RequiredArgsConstructor
public class InternalReportController {
    private static final int MAX_PAGE = 50;

    private final IAdminReportService adminReportService;
    private final CommunityApiMapper communityApiMapper;

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_learning.internal.report.read') "
            + "and @cinxServiceClient.is(authentication, 'cinx-assistant-service')")
    public ResponseEntity<PaginatedApiResponse<AdminReportResponse>> getReports(
            @RequestParam(required = false) ReportType type,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String query) {
        return ResponseEntity.ok(PaginationWrapper.wrap(adminReportService
                .getReports(type, page, Math.min(Math.max(size, 1), MAX_PAGE), query, null)
                .map(communityApiMapper::toResponse)));
    }
}
