package com.cinx.learning.enrollment.infrastructure.persistence;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity(name = "CourseAccessGrant")
@Table(name = "course_access_grant",
        uniqueConstraints = @UniqueConstraint(name = "uk_course_access_grant_source", columnNames = {"course_id", "user_id", "source", "source_reference"}))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class CourseAccessGrantJpaEntity {
    @Id
    private String id;
    @Column(name = "user_id", nullable = false)
    private String userId;
    @Column(name = "course_id", nullable = false)
    private String courseId;
    @Column(nullable = false, length = 24)
    private String source;
    @Column(name = "source_reference", nullable = false, length = 255)
    private String sourceReference;
    @Column(name = "valid_from", nullable = false)
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
    @Column(nullable = false, length = 16)
    private String status;
}
