package com.cinx.learning.catalog.api.dto.request;

import com.cinx.learning.shared.domain.CourseMembershipRole;
import jakarta.validation.constraints.NotNull;

public record UpdateCourseInstructorRequest(@NotNull CourseMembershipRole role) {
}
