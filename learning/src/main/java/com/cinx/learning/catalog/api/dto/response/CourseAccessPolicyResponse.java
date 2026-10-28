package com.cinx.learning.catalog.api.dto.response;

import com.cinx.learning.catalog.domain.CourseAccessModel;

import java.time.LocalDateTime;

public record CourseAccessPolicyResponse(
        String courseId,
        CourseAccessModel accessModel,
        String subscriptionFeatureKey,
        LocalDateTime effectiveFrom,
        String changedBy,
        String reason,
        long version
) {
}
