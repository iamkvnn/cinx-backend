package com.cinx.learning.enrollment.infrastructure.persistence;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity(name = "CourseEntitlement")
@Table(name = "course_entitlement",
        uniqueConstraints = @UniqueConstraint(name = "uk_entitlement_user_course",
                columnNames = {"user_id", "course_id"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseEntitlementJpaEntity {
    @Id
    private String id;
    @Column(name = "user_id", nullable = false)
    private String userId;
    @Column(name = "course_id", nullable = false)
    private String courseId;
    @Column(name = "source_order_id")
    private String sourceOrderId;
    @Column(name = "granted_at", nullable = false)
    private LocalDateTime grantedAt;
}
