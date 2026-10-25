package com.cinx.learning.catalog.application.query;

import com.cinx.learning.catalog.domain.CourseAccessModel;

import java.time.LocalDateTime;

public record CourseAccessPolicyView(
        String courseId,
        CourseAccessModel accessModel,
        String subscriptionFeatureKey,
        LocalDateTime effectiveFrom,
        String changedBy,
        String reason,
        long version
) {
}
