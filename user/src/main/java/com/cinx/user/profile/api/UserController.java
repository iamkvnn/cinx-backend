package com.cinx.user.profile.api;

import com.cinx.common.dto.ApiResponse;
import com.cinx.common.dto.PaginatedApiResponse;
import com.cinx.common.mapper.PaginationWrapper;
import com.cinx.common.utils.AuthenticationUtil;
import com.cinx.user.profile.api.dto.UpdateProfileRequest;
import com.cinx.user.profile.api.dto.UserDto;
import com.cinx.user.instructor.api.dto.request.InstructorApplicationRequest;
import com.cinx.user.instructor.api.dto.request.TerminatePartnershipRequest;
import com.cinx.user.profile.api.dto.request.UpdateEffectsRequest;
import com.cinx.user.profile.api.dto.request.UpdatePreferredCategoriesRequest;
import com.cinx.user.profile.api.mapper.UserMapper;
import com.cinx.user.profile.application.IUserService;
import com.cinx.user.profile.domain.InstructorApplicationStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class UserController {

    private final IUserService userService;
    private final UserMapper userMapper;

    @Operation(summary = "List all users (admin)", security = @SecurityRequirement(name = "bearer-jwt"))
    @GetMapping("/admin/profiles")
    @PreAuthorize("hasAuthority('PERM_user.profile.read.any')")
    public ResponseEntity<PaginatedApiResponse<UserDto>> getAllUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) Boolean isInstructorVerified,
            @RequestParam(required = false) InstructorApplicationStatus instructorApplicationStatus
    ) {
        return ResponseEntity.ok(
                PaginationWrapper.wrap(userService.findAll(page, size, query, isInstructorVerified,
                        instructorApplicationStatus, sort).map(userMapper::toDto))
        );
    }

    @Operation(summary = "Get user by ID", security = @SecurityRequirement(name = "bearer-jwt"))
    @GetMapping("/admin/profiles/{userId}")
    @PreAuthorize("hasAuthority('PERM_user.profile.read.any')")
    public ResponseEntity<ApiResponse<UserDto>> getUserById(@PathVariable String userId) {
        return ResponseEntity.ok(new ApiResponse<>(true, "User fetched successfully", userMapper.toDto(userService.findByUserId(userId))));
    }

    @Operation(summary = "Approve instructor (admin)", security = @SecurityRequirement(name = "bearer-jwt"))
    @PostMapping("/instructor-applications/{applicationId}/approve")
    @PreAuthorize("hasAuthority('PERM_user.instructor.review.any') and @cinxMfaVerifier.requireSatisfied(authentication)")
    public ResponseEntity<ApiResponse<?>> verifyInstructor(@PathVariable String applicationId) {
        userService.verifyInstructor(applicationId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Instructor verified successfully", null));
    }

    @Operation(summary = "Reject instructor (admin)", security = @SecurityRequirement(name = "bearer-jwt"))
    @PostMapping("/instructor-applications/{applicationId}/reject")
    @PreAuthorize("hasAuthority('PERM_user.instructor.review.any') and @cinxMfaVerifier.requireSatisfied(authentication)")
    public ResponseEntity<ApiResponse<?>> rejectInstructor(
            @PathVariable String applicationId,
            @RequestParam String reason
    ) {
        userService.rejectInstructor(applicationId, reason);
        return ResponseEntity.ok(new ApiResponse<>(true, "Instructor rejected successfully", null));
    }

    @Operation(summary = "Terminate partnership with instructor (admin)", security = @SecurityRequirement(name = "bearer-jwt"))
    @PostMapping("/instructor-partnerships/{userId}/terminate")
    @PreAuthorize("hasAuthority('PERM_user.instructor.review.any') and @cinxMfaVerifier.requireSatisfied(authentication)")
    public ResponseEntity<ApiResponse<?>> terminatePartnership(
            @PathVariable String userId,
            @Valid @RequestBody TerminatePartnershipRequest request
    ) {
        userService.terminatePartnership(userId, userMapper.toCommand(request));
        return ResponseEntity.ok(new ApiResponse<>(true, "Instructor partnership terminated successfully", null));
    }

    @Operation(summary = "Get current user profile", security = @SecurityRequirement(name = "bearer-jwt"))
    @GetMapping("/profiles/me")
    @PreAuthorize("hasAuthority('PERM_user.profile.read.self')")
    public ResponseEntity<ApiResponse<UserDto>> getCurrentUser() {
        String userId = AuthenticationUtil.extractUserId();
        return ResponseEntity.ok(new ApiResponse<>(true, "Current user fetched successfully", userMapper.toDto(userService.findByUserId(userId))));
    }

    @PostMapping("/instructor-applications")
    @PreAuthorize("hasAuthority('PERM_user.instructor.apply.self')")
    public ResponseEntity<ApiResponse<UserDto>> applyInstructor(
            @RequestBody(required = false) InstructorApplicationRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Instructor application submitted successfully",
                userMapper.toDto(userService.applyInstructor(AuthenticationUtil.extractUserId(),
                        request == null ? null : userMapper.toCommand(request)))));
    }

    @Operation(summary = "Update user profile", security = @SecurityRequirement(name = "bearer-jwt"))
    @PutMapping("/profiles/{id}")
    @PreAuthorize("hasAuthority('PERM_user.profile.update.any') or (hasAuthority('PERM_user.profile.update.self') and #id == authentication.name)")
    public ResponseEntity<ApiResponse<UserDto>> updateUser(
            @PathVariable String id,
            @Valid @RequestBody UpdateProfileRequest dto
    ) {
        return ResponseEntity.ok(new ApiResponse<>(true, "User updated successfully", userMapper.toDto(userService.updateProfile(id, userMapper.toCommand(dto)))));
    }

    @Operation(summary = "Update preferred categories", security = @SecurityRequirement(name = "bearer-jwt"))
    @PutMapping("/profiles/me/preferred-categories")
    @PreAuthorize("hasAuthority('PERM_user.profile.update.self')")
    public ApiResponse<Void> updatePreferredCategories(@Valid @RequestBody UpdatePreferredCategoriesRequest request) {
        String userId = AuthenticationUtil.extractUserId();
        userService.updatePreferredCategories(userId, request.categoryIds());
        return new ApiResponse<>(true, "Preferred categories updated successfully", null);
    }

    @Operation(summary = "Turn decorative animations on or off for the caller", security = @SecurityRequirement(name = "bearer-jwt"))
    @PutMapping("/profiles/me/effects")
    @PreAuthorize("hasAuthority('PERM_user.profile.update.self')")
    public ApiResponse<UserDto> updateEffects(@Valid @RequestBody UpdateEffectsRequest request) {
        String userId = AuthenticationUtil.extractUserId();
        return new ApiResponse<>(true, "Effects preference updated successfully", userMapper.toDto(userService.updateEffects(userId, request.enabled())));
    }
}
