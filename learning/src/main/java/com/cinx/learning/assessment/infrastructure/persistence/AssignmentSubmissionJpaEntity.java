package com.cinx.learning.assessment.infrastructure.persistence;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.List;

@Entity(name = "AssignmentSubmission")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentSubmissionJpaEntity extends UserSubmissionJpaEntity {
    private String assignmentId;
    private String content;
    private Double score;

    @Column(columnDefinition = "TEXT")
    private String feedback;

    private LocalDateTime gradedAt;
    private String gradedBy;

    @Builder.Default
    @Column(nullable = false)
    private Boolean aiAssisted = false;

    @OneToMany(mappedBy = "assignmentSubmission", fetch = FetchType.EAGER)
    private List<AssignmentSubmissionAttachmentJpaEntity> attachments;
}
