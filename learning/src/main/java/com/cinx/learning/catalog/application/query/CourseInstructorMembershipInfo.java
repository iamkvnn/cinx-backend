package com.cinx.learning.catalog.application.query;

import com.cinx.learning.shared.domain.CourseMembershipRole;
import com.cinx.learning.shared.domain.CourseMembershipStatus;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Value;
import com.cinx.learning.catalog.domain.CourseInstructorMembership;

/** What other features may read of a CourseInstructorMembership; a copy, so the domain class stays private to catalog. */
@Value
@Builder
public class CourseInstructorMembershipInfo {
    String id;
    Long version;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    String createdBy;
    String updatedBy;
    String courseId;
    String instructorId;
    CourseMembershipRole role;
    CourseMembershipStatus status;
    String invitedBy;
    LocalDateTime invitedAt;
    LocalDateTime acceptedAt;
    LocalDateTime revokedAt;

    public static CourseInstructorMembershipInfo of(CourseInstructorMembership domain) {
        return CourseInstructorMembershipInfo.builder()
                .id(domain.getId())
                .version(domain.getVersion())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .createdBy(domain.getCreatedBy())
                .updatedBy(domain.getUpdatedBy())
                .courseId(domain.getCourseId())
                .instructorId(domain.getInstructorId())
                .role(domain.getRole())
                .status(domain.getStatus())
                .invitedBy(domain.getInvitedBy())
                .invitedAt(domain.getInvitedAt())
                .acceptedAt(domain.getAcceptedAt())
                .revokedAt(domain.getRevokedAt())
                .build();
    }
}
