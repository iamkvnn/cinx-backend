package com.cinx.user.policy.api.dto.request;

import com.cinx.user.policy.domain.PolicyType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public record CreatePolicyRequest(
        @NotNull PolicyType policyType,
        @NotBlank @Size(max = 120) String slug,
        @NotBlank @Size(max = 255) String title,
        @Size(max = 1000) String summary,
        LocalDateTime effectiveAt,
        Integer displayOrder,
        @Valid @NotEmpty List<PolicySectionRequest> sections
) {
}
