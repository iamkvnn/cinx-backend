package com.cinx.user.profile.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record UpdatePreferredCategoriesRequest(
        @NotNull(message = "categoryIds must not be null")
        List<@NotBlank(message = "categoryId must not be blank") String> categoryIds
) {
}
