package com.cinx.learning.assessment.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class InVideoAssessmentSubmission extends UserSubmission {
    private String videoLessonId;
    private String videoAssessmentId;
    private String userAnswer;
}
