package com.cinx.learning.catalog.infrastructure.persistence;

import com.cinx.learning.catalog.domain.CourseAccessModel;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity(name = "CourseAccessPolicy")
@Table(name = "course_access_policy")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class CourseAccessPolicyJpaEntity {
    @Id
    @Column(name = "course_id", nullable = false)
    private String courseId;

    @Enumerated(EnumType.STRING)
    @Column(name = "access_model", nullable = false, length = 32)
    private CourseAccessModel accessModel;

    @Column(name = "subscription_feature_key", length = 160)
    private String subscriptionFeatureKey;

    @Column(name = "effective_from", nullable = false)
    private LocalDateTime effectiveFrom;
    private LocalDateTime effectiveUntil;
    @Column(name = "changed_by", nullable = false)
    private String changedBy;
    @Column(nullable = false, length = 1000)
    private String reason;
    @Version
    @Column(nullable = false)
    private long version;
}
