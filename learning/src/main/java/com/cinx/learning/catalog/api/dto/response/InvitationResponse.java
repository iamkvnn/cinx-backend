package com.cinx.learning.catalog.api.dto.response;

import com.cinx.learning.shared.domain.CourseMembershipRole;

import java.time.LocalDateTime;

/** A pending co-instructor invitation addressed to the caller. */
public record InvitationResponse(
        String courseId,
        String courseTitle,
        String thumbnailUrl,
        CourseMembershipRole role,
        LocalDateTime invitedAt,
        Inviter invitedBy
) {
    public record Inviter(String id, String name, String avatarUrl) {
    }
}
