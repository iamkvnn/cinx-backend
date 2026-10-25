package com.cinx.user.profile.api.dto;

import com.cinx.user.profile.domain.Gender;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record UpdateProfileRequest(
        @NotBlank(message = "Họ và tên không được để trống")
        @Schema(example = "Jane Doe")
        String name,
        @Schema(example = "FEMALE")
        Gender gender,
        @Schema(example = "0987654321")
        String phoneNumber,
        @Schema(example = "Senior Frontend Developer")
        String bio,
        @Schema(example = "true")
        Boolean isReceivePushNotification,
        @Schema(example = "avatar_key_123")
        String avatarFileKey,
        @Schema(example = "cv_key_123")
        String cvFileKey
) {
}
