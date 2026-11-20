package com.cinx.learning.enrollment.infrastructure.persistence;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity(name = "CourseEnrollment")
@Table(name = "course_enrollment",
        uniqueConstraints = @UniqueConstraint(name = "uk_course_enrollment_user_course", columnNames = {"user_id", "course_id"}))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class CourseEnrollmentJpaEntity {
    @Id
    private String id;
    @Column(name = "user_id", nullable = false)
    private String userId;
    @Column(name = "course_id", nullable = false)
    private String courseId;
    @Column(name = "enrolled_at", nullable = false)
    private LocalDateTime enrolledAt;
    @Column(name = "access_source_at_enrollment", nullable = false, length = 32)
    private String accessSourceAtEnrollment;
    @Column(nullable = false, length = 16)
    private String status;
}
