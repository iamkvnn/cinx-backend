package com.cinx.learning.assessment.infrastructure.persistence;


import com.cinx.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** Points a submission earned on one criterion of its assignment. */
@Entity(name = "SubmissionCriterionScore")
@Table(
        name = "submission_criterion_score",
        uniqueConstraints = @UniqueConstraint(name = "uk_submission_criterion_score", columnNames = {"submissionId", "criterionId"})
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionCriterionScoreJpaEntity extends BaseEntity {
    @Column(nullable = false)
    private String submissionId;

    @Column(nullable = false)
    private String criterionId;

    @Column(nullable = false)
    private Double points;
}
