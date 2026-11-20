package com.cinx.learning.certificate.infrastructure.persistence;


import com.cinx.common.model.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** What a learner must reach before the course's certificate is issued; absent means the defaults. */
@Entity(name = "CourseCertificateConditions")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CourseCertificateConditionsJpaEntity extends AuditableEntity {

    @Column(nullable = false, unique = true)
    private String courseId;

    @Column(nullable = false)
    private Integer minCompletionPercent;

    @Column(nullable = false)
    private Double minQuizScore;

    @Column(nullable = false)
    private Double minAssignmentScore;

    @Column(nullable = false)
    private Boolean autoApprove;
}
