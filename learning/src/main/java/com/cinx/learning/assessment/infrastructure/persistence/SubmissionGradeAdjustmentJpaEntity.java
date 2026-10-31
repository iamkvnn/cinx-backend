package com.cinx.learning.assessment.infrastructure.persistence;


import com.cinx.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A bonus (positive) or a penalty (negative) given outside the criteria, with its reason. */
@Entity(name = "SubmissionGradeAdjustment")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionGradeAdjustmentJpaEntity extends BaseEntity {
    @Column(nullable = false)
    private String submissionId;

    @Column(nullable = false)
    private Double points;

    @Column(nullable = false)
    private String reason;
}
