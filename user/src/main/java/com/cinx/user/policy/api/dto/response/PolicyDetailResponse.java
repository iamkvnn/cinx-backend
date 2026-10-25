package com.cinx.user.policy.api.dto.response;

import com.cinx.user.policy.domain.PolicyStatus;
import com.cinx.user.policy.domain.PolicyType;

import java.time.LocalDateTime;
import java.util.List;

public record PolicyDetailResponse(
        String id,
        PolicyType policyType,
        String slug,
        String title,
        String summary,
        PolicyStatus status,
        Integer versionNumber,
        LocalDateTime effectiveAt,
        LocalDateTime publishedAt,
        Integer displayOrder,
        List<PolicySectionResponse> sections
) {
}
