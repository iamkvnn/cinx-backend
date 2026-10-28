package com.cinx.learning.catalog.api.dto.response;

import com.cinx.learning.shared.domain.CourseMembershipRole;
import com.cinx.learning.shared.domain.CourseMembershipStatus;

import java.time.LocalDateTime;

public record CourseInstructorMembershipResponse(
        String instructorId,
        CourseMembershipRole role,
        CourseMembershipStatus status,
        String invitedBy,
        LocalDateTime invitedAt,
        LocalDateTime acceptedAt,
        LocalDateTime revokedAt,
        boolean primary,
        String name,
        String avatarUrl
) {
}
