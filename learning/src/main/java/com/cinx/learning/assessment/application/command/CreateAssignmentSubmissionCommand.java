package com.cinx.learning.assessment.application.command;

import java.util.List;

public record CreateAssignmentSubmissionCommand(
    String content,

    List<AttachmentRequest> attachments
) {
    public record AttachmentRequest(
        String fileKey,

        String fileName,

        String fileType,

        Long fileSize
    ){}
}
