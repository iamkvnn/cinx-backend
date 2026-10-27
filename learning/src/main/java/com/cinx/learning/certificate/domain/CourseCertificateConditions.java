package com.cinx.learning.certificate.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import com.cinx.learning.shared.domain.AuditedModel;

/** What a learner must reach before the course's certificate is issued; absent means the defaults. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CourseCertificateConditions extends AuditedModel {
    public static final int DEFAULT_MIN_COMPLETION_PERCENT = 100;
    public static final double DEFAULT_MIN_QUIZ_SCORE = 5.0;
    public static final double DEFAULT_MIN_ASSIGNMENT_SCORE = 5.0;
    private String courseId;
    private Integer minCompletionPercent;
    private Double minQuizScore;
    private Double minAssignmentScore;
    private Boolean autoApprove;

    public static CourseCertificateConditions defaults(String courseId) {
        return CourseCertificateConditions.builder()
                .courseId(courseId)
                .minCompletionPercent(DEFAULT_MIN_COMPLETION_PERCENT)
                .minQuizScore(DEFAULT_MIN_QUIZ_SCORE)
                .minAssignmentScore(DEFAULT_MIN_ASSIGNMENT_SCORE)
                .autoApprove(false)
                .build();
    }
}
