package com.cinx.learning.assessment.infrastructure.persistence;


import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.*;

@Entity(name = "SubmissionResult")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionResultJpaEntity {
    @Id
    private String submissionId;
    private Double score;
    private String feedback;
}
