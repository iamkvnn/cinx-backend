package com.cinx.learning.catalog.application.command;

import com.cinx.learning.catalog.domain.CourseAccessModel;

public record UpdateCourseAccessPolicyCommand(
         CourseAccessModel accessModel,
        String subscriptionFeatureKey,
         String reason
) {
}
