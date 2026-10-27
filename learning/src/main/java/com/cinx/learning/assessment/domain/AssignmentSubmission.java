package com.cinx.learning.assessment.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentSubmission extends UserSubmission {
    private String assignmentId;
    private String content;
    private Double score;
    private String feedback;
    private LocalDateTime gradedAt;
    private String gradedBy;
    /** The grade started from an AI suggestion; the instructor still set it. */
    @Builder.Default
    private Boolean aiAssisted = false;
    /** The files the learner attached; read from the store, and written through their own repository. */
    @Builder.Default
    private List<AssignmentSubmissionAttachment> attachments = new ArrayList<>();
}
