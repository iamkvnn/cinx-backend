package com.cinx.user.profile.api;

import com.cinx.common.dto.ApiResponse;
import com.cinx.common.utils.AuthenticationUtil;
import com.cinx.user.profile.domain.AccountType;
import com.cinx.user.profile.api.dto.UserDto;
import com.cinx.user.profile.api.dto.request.OnboardProfileRequest;
import com.cinx.user.profile.api.mapper.UserMapper;
import com.cinx.user.profile.application.IUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/profiles")
@RequiredArgsConstructor
public class ProfileOnboardingController {
    private final IUserService userService;
    private final UserMapper userMapper;

    @PostMapping("/me/onboarding")
    @Operation(security = @SecurityRequirement(name = "bearer-jwt"))
    public ApiResponse<UserDto> onboard(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody OnboardProfileRequest request) {
        UserDto profile = userMapper.toDto(userService.onboardProfile(
                AuthenticationUtil.extractUserId(),
                AuthenticationUtil.extractSubject(),
                AuthenticationUtil.extractEmail(),
                AccountType.STUDENT,
                userMapper.toCommand(request),
                idempotencyKey));
        return new ApiResponse<>(true, "Profile onboarded successfully", profile);
    }
}
