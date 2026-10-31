package com.cinx.learning.assessment.infrastructure.persistence;


import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity(name = "InVideoAssessmentSubmission")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "video_assessment_id"}))
public class InVideoAssessmentSubmissionJpaEntity extends UserSubmissionJpaEntity {
    private String videoLessonId;
    private String videoAssessmentId;
    private String userAnswer;
}
