package com.cinx.user.profile.api.dto;

import com.cinx.user.profile.domain.Gender;
import com.cinx.user.profile.domain.AccountType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateUserRequest(
        @NotBlank(message = "Họ và tên không được để trống")
        @Schema(example = "user_123")
        String userId,
        @NotBlank(message = "Họ và tên không được để trống")
        @Schema(example = "John Doe")
        String name,
        @NotBlank(message = "Email không được để trống")
        @Email(message = "Email không hợp lệ")
        @Schema(example = "johndoe@example.com")
        String email,
        @Schema(example = "STUDENT")
        AccountType accountType,
        @Schema(example = "MALE")
        Gender gender,
        @Schema(example = "0987654321")
        String phoneNumber,
        @Schema(example = "Senior Frontend Developer")
        String bio,
        @Schema(example = "cv_key_123")
        String cvFileKey
) {
}
