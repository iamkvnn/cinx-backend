package com.cinx.reporting.analytics.api;

import com.cinx.common.dto.ApiResponse;
import com.cinx.reporting.analytics.api.dto.response.EventDefinitionResponse;
import com.cinx.reporting.analytics.api.dto.response.FunnelResponse;
import com.cinx.reporting.analytics.api.mapper.AnalyticsMapper;
import com.cinx.reporting.analytics.application.IFunnelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/reporting")
@RequiredArgsConstructor
public class FunnelController {
    private static final String READ = "hasAuthority('PERM_reporting.dashboard.read') and @cinxMfaVerifier.requireSatisfied(authentication)";

    private final IFunnelService funnels;
    private final AnalyticsMapper mapper;

    @GetMapping("/funnels")
    @PreAuthorize(READ)
    @Operation(summary = "Users through ordered steps between two Vietnam days", security = @SecurityRequirement(name = "bearer-jwt"))
    public ApiResponse<FunnelResponse> funnel(
            @Parameter(description = "2-5 event names, in order", example = "recommendation_shown,recommendation_clicked")
            @RequestParam List<String> steps,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return new ApiResponse<>(true, "Funnel fetched successfully", mapper.toDto(funnels.funnel(steps, from, to)));
    }

    @GetMapping("/events/catalog")
    @PreAuthorize(READ)
    @Operation(summary = "The event dictionary, for the step and goal pickers", security = @SecurityRequirement(name = "bearer-jwt"))
    public ApiResponse<List<EventDefinitionResponse>> catalog() {
        return new ApiResponse<>(true, "Event catalog fetched successfully", mapper.toCatalog(funnels.catalog()));
    }
}
