package com.cinx.notification.device.api;

import com.cinx.common.dto.ApiResponse;
import com.cinx.common.utils.AuthenticationUtil;
import com.cinx.notification.device.api.dto.request.DeviceTokenRequest;
import com.cinx.notification.device.application.IDeviceTokenService;
import com.cinx.notification.device.application.command.RegisterDeviceTokenCommand;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications/device-tokens")
@RequiredArgsConstructor
public class DeviceTokenController {
    private final IDeviceTokenService deviceTokenService;

    @PostMapping
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('PERM_notification.update.self')")
    @Operation(security = @SecurityRequirement(name = "bearer-jwt"))
    ApiResponse<Void> register(@Valid @RequestBody DeviceTokenRequest request) {
        deviceTokenService.register(AuthenticationUtil.extractUserId(), new RegisterDeviceTokenCommand(request.fcmToken(), request.deviceInfo()));
        return new ApiResponse<>(true, "Device token registered successfully", null);
    }
}
