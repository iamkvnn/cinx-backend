package com.cinx.learning.assessment.infrastructure.persistence;


import com.cinx.common.model.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** One criterion of an assignment; the criteria of an assignment add up to 10 points. */
@Entity(name = "AssignmentCriterion")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentCriterionJpaEntity extends AuditableEntity {
    @Column(nullable = false)
    private String assignmentId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private Double maxPoints;

    @Column(nullable = false)
    private Integer orderIndex;
}
