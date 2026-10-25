package com.cinx.learning.catalog.application.command;

import com.cinx.learning.shared.domain.CourseMembershipRole;

public record InviteCourseInstructorCommand(
         String instructorId,
         CourseMembershipRole role
) {
}
