package com.cinx.learning.assessment.application.query;

public record AssignmentSubmissionAttachmentView(
        String id,
        String fileName,
        String fileType,
        long fileSize,
        String attachmentUrl
) {
}
