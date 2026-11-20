package com.cinx.reporting.reporting.api;

import com.cinx.common.dto.ApiResponse;
import com.cinx.reporting.reporting.api.dto.FinanceOverviewResponse;
import com.cinx.reporting.reporting.api.dto.ReportingOverviewResponse;
import com.cinx.reporting.reporting.api.mapper.ReportingOverviewMapper;
import com.cinx.reporting.reporting.application.ReportingQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reporting")
@RequiredArgsConstructor
public class ReportingController {
    private final ReportingQueryService reportingQueryService;
    private final ReportingOverviewMapper mapper;

    @GetMapping("/overview")
    @PreAuthorize("hasAuthority('PERM_reporting.dashboard.read') and @cinxMfaVerifier.requireSatisfied(authentication)")
    public ApiResponse<ReportingOverviewResponse> overview() {
        return new ApiResponse<>(true, "Reporting overview fetched successfully",
                mapper.toDto(reportingQueryService.dashboardOverview()));
    }

    @GetMapping("/finance/overview")
    @PreAuthorize("hasAuthority('PERM_reporting.finance.read') and @cinxMfaVerifier.requireSatisfied(authentication)")
    public ApiResponse<FinanceOverviewResponse> financeOverview() {
        return new ApiResponse<>(true, "Finance overview fetched successfully",
                mapper.toDto(reportingQueryService.financeOverview()));
    }
}
