package com.cinx.learning.assessment.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import com.cinx.learning.shared.domain.Model;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentSubmissionAttachment extends Model {
    private String attachmentUrl;
    private String fileKey;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private String assignmentSubmissionId;
}
