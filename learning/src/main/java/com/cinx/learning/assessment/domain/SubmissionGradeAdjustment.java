package com.cinx.learning.assessment.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import com.cinx.learning.shared.domain.Model;

/** A bonus (positive) or a penalty (negative) given outside the criteria, with its reason. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionGradeAdjustment extends Model {
    private String submissionId;
    private Double points;
    private String reason;
}
