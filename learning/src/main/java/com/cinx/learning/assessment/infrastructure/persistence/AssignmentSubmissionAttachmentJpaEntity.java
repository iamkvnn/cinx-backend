package com.cinx.learning.assessment.infrastructure.persistence;


import com.cinx.common.model.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity(name = "AssignmentSubmissionAttachment")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentSubmissionAttachmentJpaEntity extends BaseEntity {
    private String attachmentUrl;
    private String fileKey;
    private String fileName;
    private String fileType;
    private Long fileSize;

    @ManyToOne
    private AssignmentSubmissionJpaEntity assignmentSubmission;
}
