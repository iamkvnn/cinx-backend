package com.cinx.learning.assessment.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import com.cinx.learning.shared.domain.AuditedModel;

/** An instructor's comment on one line of a submitted text file. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionLineComment extends AuditedModel {
    private String submissionId;
    private String attachmentId;
    private Integer line;
    private String content;
    private String authorId;
}
