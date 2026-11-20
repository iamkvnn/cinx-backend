package com.cinx.auth.identity.api;

import com.cinx.auth.identity.application.IIdentityService;

import com.cinx.auth.identity.api.dto.request.BanUserRequest;
import com.cinx.auth.identity.api.dto.IdentityAccountResponse;
import com.cinx.auth.identity.api.mapper.IdentityAccountMapper;
import com.cinx.common.dto.ApiResponse;
import com.cinx.common.utils.AuthenticationUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/identity/accounts")
@RequiredArgsConstructor
public class IdentityController {
    private final IIdentityService identityService;
    private final IdentityAccountMapper mapper;

    @GetMapping("/{userId}")
    @PreAuthorize("hasAuthority('PERM_auth.account.read.any')")
    @Operation(security = @SecurityRequirement(name = "bearer-jwt"))
    ApiResponse<IdentityAccountResponse> findAccount(@PathVariable String userId) {
        return ApiResponse.success("Account fetched successfully", mapper.toDto(identityService.findAccount(userId)));
    }

    @PostMapping("/{userId}/suspend")
    @PreAuthorize("hasAuthority('PERM_auth.account.suspend.any') and @cinxMfaVerifier.requireSatisfied(authentication)")
    @Operation(security = @SecurityRequirement(name = "bearer-jwt"))
    ApiResponse<IdentityAccountResponse> suspend(
            @PathVariable String userId,
            @Valid @RequestBody BanUserRequest request
    ) {
        return new ApiResponse<>(true, "Account suspended successfully",
                mapper.toDto(identityService.ban(userId, mapper.toCommand(request))));
    }

    @PostMapping("/{userId}/reactivate")
    @PreAuthorize("hasAuthority('PERM_auth.account.reactivate.any') and @cinxMfaVerifier.requireSatisfied(authentication)")
    @Operation(security = @SecurityRequirement(name = "bearer-jwt"))
    ApiResponse<IdentityAccountResponse> reactivate(@PathVariable String userId) {
        return new ApiResponse<>(true, "Account reactivated successfully", mapper.toDto(identityService.unban(userId)));
    }

    @PutMapping("/{userId}/roles")
    @PreAuthorize("hasAuthority('PERM_auth.role.assign.any') and @cinxMfaVerifier.requireSatisfied(authentication)")
    @Operation(security = @SecurityRequirement(name = "bearer-jwt"))
    ApiResponse<IdentityAccountResponse> updateRoles(
            @PathVariable String userId,
            @Valid @RequestBody com.cinx.auth.identity.api.dto.UpdateRolesRequest request
    ) {
        return ApiResponse.success("Account roles updated successfully",
                mapper.toDto(identityService.updateStaffRoles(AuthenticationUtil.extractUserId(), userId, mapper.toCommand(request))));
    }
}
