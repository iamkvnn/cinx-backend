package com.cinx.learning.catalog.infrastructure.persistence;

import com.cinx.learning.shared.domain.CourseMembershipRole;
import com.cinx.learning.shared.domain.CourseMembershipStatus;

import com.cinx.common.model.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity(name = "CourseInstructorMembership")
@Table(
        name = "course_instructor_membership",
        uniqueConstraints = @UniqueConstraint(name = "uk_course_instructor_membership", columnNames = {"course_id", "instructor_id"}),
        indexes = {
                @Index(name = "idx_membership_instructor_status", columnList = "instructor_id,status"),
                @Index(name = "idx_membership_course_status", columnList = "course_id,status")
        }
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CourseInstructorMembershipJpaEntity extends AuditableEntity {
    @Column(name = "course_id", nullable = false, length = 255)
    private String courseId;

    @Column(name = "instructor_id", nullable = false, length = 255)
    private String instructorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CourseMembershipRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CourseMembershipStatus status;

    @Column(name = "invited_by", length = 255)
    private String invitedBy;

    @Column(name = "invited_at")
    private LocalDateTime invitedAt;

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;
}
