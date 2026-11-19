package com.cinx.reporting.reporting.api.internal;

import com.cinx.common.dto.ApiResponse;
import com.cinx.reporting.reporting.api.dto.FinanceOverviewResponse;
import com.cinx.reporting.reporting.api.dto.ReportingOverviewResponse;
import com.cinx.reporting.reporting.api.mapper.ReportingOverviewMapper;
import com.cinx.reporting.reporting.application.ReportingQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The same overviews as /api/v1/reporting, for the admin copilot. The assistant checks the admin's reporting
 * permission and a fresh MFA before calling, and audits each call; that is why these carry no MFA check here.
 */
@RestController
@RequestMapping("/internal/reporting")
@RequiredArgsConstructor
public class InternalReportingController {
    private static final String ASSISTANT = "@cinxServiceClient.is(authentication, 'cinx-assistant-service')";

    private final ReportingQueryService reportingQueryService;
    private final ReportingOverviewMapper mapper;

    @GetMapping("/overview")
    @PreAuthorize("hasAuthority('PERM_reporting.internal.overview.read') and " + ASSISTANT)
    public ApiResponse<ReportingOverviewResponse> overview() {
        return new ApiResponse<>(true, "Reporting overview fetched successfully", mapper.toDto(reportingQueryService.dashboardOverview()));
    }

    @GetMapping("/finance/overview")
    @PreAuthorize("hasAuthority('PERM_reporting.internal.overview.read') and " + ASSISTANT)
    public ApiResponse<FinanceOverviewResponse> financeOverview() {
        return new ApiResponse<>(true, "Finance overview fetched successfully", mapper.toDto(reportingQueryService.financeOverview()));
    }
}
