package com.cinx.learning.catalog.api.dto.request;

import com.cinx.learning.catalog.domain.CourseAccessModel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateCourseAccessPolicyRequest(
        @NotNull CourseAccessModel accessModel,
        String subscriptionFeatureKey,
        @NotBlank String reason
) {
}
