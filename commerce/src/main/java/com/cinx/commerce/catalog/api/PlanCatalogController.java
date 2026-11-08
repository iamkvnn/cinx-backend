package com.cinx.commerce.catalog.api;

import com.cinx.commerce.catalog.api.mapper.PlanCatalogMapper;
import com.cinx.commerce.catalog.application.PlanCatalogService;

import com.cinx.commerce.catalog.api.dto.PlanDraftRequest;
import com.cinx.commerce.catalog.api.dto.PublishedPlanResponse;
import com.cinx.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
public class PlanCatalogController {
    private final PlanCatalogService service;
    private final PlanCatalogMapper mapper;

    @GetMapping("/api/v1/plans")
    public ResponseEntity<ApiResponse<List<PublishedPlanResponse>>> list(
            @RequestParam(defaultValue = "VN") String market,
            @RequestParam(required = false) String locale) {
        return ResponseEntity.ok(ApiResponse.success("Plans fetched successfully", service.publishedPlans(market, locale).stream().map(mapper::toResponse).toList()));
    }

    @GetMapping("/api/v1/plans/{planCode}")
    public ResponseEntity<ApiResponse<PublishedPlanResponse>> get(
            @PathVariable String planCode,
            @RequestParam(defaultValue = "VN") String market,
            @RequestParam(required = false) String locale) {
        return ResponseEntity.ok(ApiResponse.success("Plan fetched successfully", mapper.toResponse(service.publishedPlan(planCode, market, locale))));
    }

    @PostMapping("/api/v1/admin/plans")
    @PreAuthorize("hasAuthority('PERM_commerce.plan.manage.any') and @cinxMfaVerifier.requireSatisfied(authentication)")
    public ResponseEntity<ApiResponse<PublishedPlanResponse>> createDraft(@Valid @RequestBody PlanDraftRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Plan draft created successfully", mapper.toResponse(service.createDraft(mapper.toCommand(request)))));
    }

    @PostMapping("/api/v1/admin/plans/{planCode}/versions/{version}/publish")
    @PreAuthorize("hasAuthority('PERM_commerce.plan.manage.any') and @cinxMfaVerifier.requireSatisfied(authentication)")
    public ResponseEntity<ApiResponse<PublishedPlanResponse>> publish(
            @PathVariable String planCode,
            @PathVariable long version) {
        return ResponseEntity.ok(ApiResponse.success("Plan version published successfully",
                mapper.toResponse(service.publish(planCode, version, com.cinx.common.utils.AuthenticationUtil.extractUserId()))));
    }

    @PostMapping("/api/v1/admin/plans/{planCode}/versions/{version}/validate")
    @PreAuthorize("hasAuthority('PERM_commerce.plan.manage.any') and @cinxMfaVerifier.requireSatisfied(authentication)")
    public ResponseEntity<ApiResponse<PublishedPlanResponse>> validate(
            @PathVariable String planCode, @PathVariable long version) {
        return ResponseEntity.ok(ApiResponse.success("Plan version validated successfully",
                mapper.toResponse(service.validate(planCode, version))));
    }

    @PostMapping("/api/v1/admin/plans/{planCode}/versions/{version}/retire")
    @PreAuthorize("hasAuthority('PERM_commerce.plan.manage.any') and @cinxMfaVerifier.requireSatisfied(authentication)")
    public ResponseEntity<ApiResponse<Void>> retire(@PathVariable String planCode, @PathVariable long version) {
        service.retire(planCode, version, com.cinx.common.utils.AuthenticationUtil.extractUserId());
        return ResponseEntity.ok(ApiResponse.success("Plan version retired successfully", null));
    }
}
