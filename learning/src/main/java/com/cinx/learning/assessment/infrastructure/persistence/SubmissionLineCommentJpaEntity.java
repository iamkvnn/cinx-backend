package com.cinx.learning.assessment.infrastructure.persistence;


import com.cinx.common.model.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** An instructor's comment on one line of a submitted text file. */
@Entity(name = "SubmissionLineComment")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionLineCommentJpaEntity extends AuditableEntity {
    @Column(nullable = false)
    private String submissionId;

    @Column(nullable = false)
    private String attachmentId;

    @Column(nullable = false)
    private Integer line;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(nullable = false)
    private String authorId;
}
