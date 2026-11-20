package com.cinx.learning.catalog.api.dto.request;

import com.cinx.learning.shared.domain.CourseMembershipRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record InviteCourseInstructorRequest(
        @NotBlank String instructorId,
        @NotNull CourseMembershipRole role
) {
}
