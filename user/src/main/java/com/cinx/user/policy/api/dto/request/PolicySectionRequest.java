package com.cinx.user.policy.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PolicySectionRequest(
        @NotBlank @Size(max = 255) String heading,
        @Size(max = 255) String anchor,
        @NotBlank String bodyMarkdown,
        @NotNull Integer orderIndex
) {
}
