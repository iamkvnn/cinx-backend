package com.cinx.learning.catalog.domain;

import com.cinx.learning.shared.domain.CourseMembershipStatus;

import com.cinx.learning.shared.domain.CourseMembershipRole;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import com.cinx.learning.shared.domain.AuditedModel;
import java.time.LocalDateTime;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CourseInstructorMembership extends AuditedModel {
    private String courseId;
    private String instructorId;
    private CourseMembershipRole role;
    private CourseMembershipStatus status;
    private String invitedBy;
    private LocalDateTime invitedAt;
    private LocalDateTime acceptedAt;
    private LocalDateTime revokedAt;
}
