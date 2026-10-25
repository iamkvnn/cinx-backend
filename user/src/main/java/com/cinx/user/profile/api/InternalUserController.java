package com.cinx.user.profile.api;

import com.cinx.common.dto.ApiResponse;
import com.cinx.user.profile.api.dto.CreateUserRequest;
import com.cinx.user.instructor.api.dto.InstructorLookupDto;
import com.cinx.user.profile.api.dto.UserDto;
import com.cinx.user.profile.api.mapper.UserMapper;
import com.cinx.user.profile.application.IUserService;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Internal API — called only by other services via Feign (service-to-service).
 * Not exposed externally; blocked at the gateway layer (/internal/** → denyAll).
 */
@Hidden
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InternalUserController {

    private final IUserService userService;
    private final UserMapper userMapper;

    @PostMapping("/users")
    public ApiResponse<UserDto> createUser(@RequestBody CreateUserRequest request) {
        return new ApiResponse<>(true, "User created successfully", userMapper.toDto(userService.createUser(userMapper.toCommand(request))));
    }

    @GetMapping("/users/{id}")
    @PreAuthorize("hasAuthority('PERM_user.internal.profile.read')")
    public ApiResponse<UserDto> getUserById(@PathVariable String id) {
        return new ApiResponse<>(true, "User fetched successfully", userMapper.toDto(userService.findByUserId(id)));
    }

    @GetMapping("/user-ids")
    @PreAuthorize("hasAuthority('PERM_user.internal.profile.read')")
    public ApiResponse<List<UserDto>> getUsersByIds(@RequestParam List<String> ids) {
        return new ApiResponse<>(true, "Users fetched successfully", userService.findByIds(ids).stream().map(userMapper::toDto).toList());
    }

    /** Exact-email lookup of one verified instructor; 404 INSTRUCTOR_NOT_FOUND otherwise (no directory). */
    @GetMapping("/users/instructor-lookup")
    @PreAuthorize("hasAuthority('PERM_user.internal.profile.read')")
    public ApiResponse<InstructorLookupDto> lookupInstructor(@RequestParam String email) {
        return new ApiResponse<>(true, "Instructor fetched successfully", userMapper.toDto(userService.lookupVerifiedInstructor(email)));
    }

    @GetMapping("/users/{userId}/instructor-verified")
    @PreAuthorize("hasAuthority('PERM_user.internal.profile.read')")
    public ApiResponse<Boolean> checkInstructorVerified(@PathVariable String userId) {
        return new ApiResponse<>(true, "Instructor verification status fetched successfully",
                userService.checkInstructorVerified(userId));
    }

    @PostMapping("/users/{userId}/last-access")
    public ApiResponse<Void> updateLastAccess(@PathVariable String userId) {
        userService.updateLastAccess(userId);
        return new ApiResponse<>(true, "Last access updated successfully", null);
    }

    @GetMapping("/users/metrics/total-count")
    public ApiResponse<Long> getTotalUsersCount() {
        return new ApiResponse<>(true, "Total users count fetched successfully", userService.countTotalUsers());
    }

    @GetMapping("/users/metrics/new-count")
    public ApiResponse<Long> getNewUsersCount(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return new ApiResponse<>(true, "New users count fetched successfully", userService.countUsersBetween(start, end));
    }
}
