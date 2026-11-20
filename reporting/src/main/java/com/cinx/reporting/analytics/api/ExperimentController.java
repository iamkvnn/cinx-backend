package com.cinx.reporting.analytics.api;

import com.cinx.common.dto.ApiResponse;
import com.cinx.common.utils.AuthenticationUtil;
import com.cinx.reporting.analytics.api.dto.request.CreateExperimentRequest;
import com.cinx.reporting.analytics.api.dto.response.AssignmentResponse;
import com.cinx.reporting.analytics.api.dto.response.ExperimentResponse;
import com.cinx.reporting.analytics.api.dto.response.ExperimentResultsResponse;
import com.cinx.reporting.analytics.api.mapper.AnalyticsMapper;
import com.cinx.reporting.analytics.application.IExperimentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Experiments: the learner's assignments, and the admin's drafts, starts, stops (the kill switch) and results. */
@RestController
@RequestMapping("/api/v1/reporting/experiments")
@RequiredArgsConstructor
public class ExperimentController {
    private static final String MANAGE = "hasAuthority('PERM_reporting.experiment.manage.any') and @cinxMfaVerifier.requireSatisfied(authentication)";

    private final IExperimentService experiments;
    private final AnalyticsMapper mapper;

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "My variants of the running experiments", security = @SecurityRequirement(name = "bearer-jwt"))
    public ApiResponse<List<AssignmentResponse>> mine() {
        return new ApiResponse<>(true, "Assignments fetched successfully",
                mapper.toAssignments(experiments.assignments(AuthenticationUtil.extractUserId())));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('PERM_reporting.dashboard.read', 'PERM_reporting.experiment.manage.any') and @cinxMfaVerifier.requireSatisfied(authentication)")
    @Operation(summary = "All experiments, newest first", security = @SecurityRequirement(name = "bearer-jwt"))
    public ApiResponse<List<ExperimentResponse>> list() {
        return new ApiResponse<>(true, "Experiments fetched successfully", mapper.toExperiments(experiments.list()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(MANAGE)
    @Operation(summary = "Create a draft experiment", security = @SecurityRequirement(name = "bearer-jwt"))
    public ApiResponse<ExperimentResponse> create(@Valid @RequestBody CreateExperimentRequest request) {
        return new ApiResponse<>(true, "Experiment created",
                mapper.toDto(experiments.create(mapper.toCommand(request), AuthenticationUtil.extractUserId())));
    }

    @PostMapping("/{key}/start")
    @PreAuthorize(MANAGE)
    @Operation(summary = "Start a draft experiment; its variants are frozen from now", security = @SecurityRequirement(name = "bearer-jwt"))
    public ApiResponse<ExperimentResponse> start(@PathVariable String key) {
        return new ApiResponse<>(true, "Experiment started",
                mapper.toDto(experiments.start(key, AuthenticationUtil.extractUserId())));
    }

    @PostMapping("/{key}/stop")
    @PreAuthorize(MANAGE)
    @Operation(summary = "Stop a running experiment: everyone gets control at once", security = @SecurityRequirement(name = "bearer-jwt"))
    public ApiResponse<ExperimentResponse> stop(@PathVariable String key) {
        return new ApiResponse<>(true, "Experiment stopped",
                mapper.toDto(experiments.stop(key, AuthenticationUtil.extractUserId())));
    }

    @GetMapping("/{key}/results")
    @PreAuthorize("hasAuthority('PERM_reporting.dashboard.read') and @cinxMfaVerifier.requireSatisfied(authentication)")
    @Operation(summary = "Exposed and converted learners per variant", security = @SecurityRequirement(name = "bearer-jwt"))
    public ApiResponse<ExperimentResultsResponse> results(@PathVariable String key) {
        return new ApiResponse<>(true, "Experiment results fetched successfully", mapper.toDto(experiments.results(key)));
    }
}
