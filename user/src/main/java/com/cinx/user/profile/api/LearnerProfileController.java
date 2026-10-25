package com.cinx.user.profile.api;

import com.cinx.common.dto.ApiResponse;
import com.cinx.common.utils.AuthenticationUtil;
import com.cinx.user.profile.api.dto.LearnerProfileDto;
import com.cinx.user.profile.api.dto.request.LearnerProfileRequest;
import com.cinx.user.profile.api.mapper.UserMapper;
import com.cinx.user.profile.application.ILearnerProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The learner's own learning profile (P1-06): what they learn for, level, daily time, language, topics, consent. */
@RestController
@RequestMapping("/api/v1/profiles/me/learning-profile")
@RequiredArgsConstructor
public class LearnerProfileController {
    private final ILearnerProfileService learnerProfiles;
    private final UserMapper userMapper;

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_user.profile.read.self')")
    @Operation(summary = "My learning profile", security = @SecurityRequirement(name = "bearer-jwt"))
    public ApiResponse<LearnerProfileDto> get() {
        return new ApiResponse<>(true, "Learning profile fetched successfully",
                userMapper.toDto(learnerProfiles.get(AuthenticationUtil.extractUserId())));
    }

    @PutMapping
    @PreAuthorize("hasAuthority('PERM_user.profile.update.self')")
    @Operation(summary = "Save my learning profile", security = @SecurityRequirement(name = "bearer-jwt"))
    public ApiResponse<LearnerProfileDto> save(@Valid @RequestBody LearnerProfileRequest request) {
        return new ApiResponse<>(true, "Learning profile saved successfully",
                userMapper.toDto(learnerProfiles.save(AuthenticationUtil.extractUserId(), userMapper.toCommand(request))));
    }

    @PostMapping("/skip")
    @PreAuthorize("hasAuthority('PERM_user.profile.update.self')")
    @Operation(summary = "Answer my learning profile later", security = @SecurityRequirement(name = "bearer-jwt"))
    public ApiResponse<LearnerProfileDto> skip() {
        return new ApiResponse<>(true, "Learning profile skipped",
                userMapper.toDto(learnerProfiles.skip(AuthenticationUtil.extractUserId())));
    }
}
