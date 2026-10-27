package com.cinx.learning.assessment.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import com.cinx.learning.shared.domain.AuditedModel;

/** One criterion of an assignment; the criteria of an assignment add up to 10 points. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentCriterion extends AuditedModel {
    private String assignmentId;
    private String title;
    private String description;
    private Double maxPoints;
    private Integer orderIndex;
}
