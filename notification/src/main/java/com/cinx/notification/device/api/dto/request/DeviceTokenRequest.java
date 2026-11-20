package com.cinx.notification.device.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record DeviceTokenRequest(@NotBlank String fcmToken, String deviceInfo) {
}
