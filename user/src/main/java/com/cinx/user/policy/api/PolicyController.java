package com.cinx.user.policy.api;

import com.cinx.common.dto.ApiResponse;
import com.cinx.common.dto.PaginatedApiResponse;
import com.cinx.common.mapper.PaginationWrapper;
import com.cinx.user.policy.domain.PolicyStatus;
import com.cinx.user.policy.domain.PolicyType;
import com.cinx.user.policy.api.dto.request.CreatePolicyRequest;
import com.cinx.user.policy.api.dto.request.UpdatePolicyRequest;
import com.cinx.user.policy.api.dto.response.PolicyDetailResponse;
import com.cinx.user.policy.api.dto.response.PolicySummaryResponse;
import com.cinx.user.policy.api.mapper.PolicyMapper;
import com.cinx.user.policy.application.IPolicyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/policies")
@RequiredArgsConstructor
public class PolicyController {
    private final IPolicyService policyService;
    private final PolicyMapper mapper;

    @GetMapping
    public ResponseEntity<ApiResponse<List<PolicySummaryResponse>>> getPublishedPolicies() {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Policies fetched successfully",
                policyService.findPublishedPolicies().stream().map(mapper::toSummaryResponse).toList()
        ));
    }

    @Operation(summary = "List policy versions", security = @SecurityRequirement(name = "bearer-jwt"))
    @GetMapping("/versions")
    @PreAuthorize("hasAuthority('PERM_user.policy.manage.any')")
    public ResponseEntity<PaginatedApiResponse<PolicySummaryResponse>> getPolicyVersions(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) PolicyStatus status,
            @RequestParam(required = false) PolicyType policyType,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String sort
    ) {
        return ResponseEntity.ok(PaginationWrapper.wrap(
                policyService.findAllVersions(page, size, status, policyType, query, sort).map(mapper::toSummaryResponse)
        ));
    }

    @GetMapping("/{slug}")
    public ResponseEntity<ApiResponse<PolicyDetailResponse>> getPublishedPolicy(@PathVariable String slug) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Policy fetched successfully",
                mapper.toDetailResponse(policyService.findPublishedPolicyBySlug(slug))
        ));
    }

    @Operation(summary = "Get policy detail by ID (admin)", security = @SecurityRequirement(name = "bearer-jwt"))
    @GetMapping("/detail/{id}")
    @PreAuthorize("hasAuthority('PERM_user.policy.manage.any')")
    public ResponseEntity<ApiResponse<PolicyDetailResponse>> getPolicyDetail(@PathVariable String id) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Policy details fetched successfully",
                mapper.toDetailResponse(policyService.findById(id))
        ));
    }

    @Operation(summary = "Create policy draft", security = @SecurityRequirement(name = "bearer-jwt"))
    @PostMapping
    @PreAuthorize("hasAuthority('PERM_user.policy.manage.any')")
    public ResponseEntity<ApiResponse<PolicyDetailResponse>> createPolicyDraft(
            @Valid @RequestBody CreatePolicyRequest request
    ) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Policy draft created successfully",
                mapper.toDetailResponse(policyService.createDraft(mapper.toCommand(request)))
        ));
    }

    @Operation(summary = "Update policy draft", security = @SecurityRequirement(name = "bearer-jwt"))
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_user.policy.manage.any')")
    public ResponseEntity<ApiResponse<PolicyDetailResponse>> updatePolicyDraft(
            @PathVariable String id,
            @Valid @RequestBody UpdatePolicyRequest request
    ) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Policy draft updated successfully",
                mapper.toDetailResponse(policyService.updateDraft(id, mapper.toCommand(request)))
        ));
    }

    @Operation(summary = "Publish policy draft", security = @SecurityRequirement(name = "bearer-jwt"))
    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAuthority('PERM_user.policy.publish.any') and @cinxMfaVerifier.requireSatisfied(authentication)")
    public ResponseEntity<ApiResponse<PolicyDetailResponse>> publishPolicy(@PathVariable String id) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Policy published successfully",
                mapper.toDetailResponse(policyService.publish(id))
        ));
    }

    @Operation(summary = "Archive policy", security = @SecurityRequirement(name = "bearer-jwt"))
    @PostMapping("/{id}/archive")
    @PreAuthorize("hasAuthority('PERM_user.policy.manage.any')")
    public ResponseEntity<ApiResponse<PolicyDetailResponse>> archivePolicy(@PathVariable String id) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Policy archived successfully",
                mapper.toDetailResponse(policyService.archive(id))
        ));
    }
}
