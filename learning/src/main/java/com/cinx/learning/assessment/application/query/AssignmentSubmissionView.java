package com.cinx.learning.assessment.application.query;

import java.time.LocalDateTime;
import java.util.List;

public record AssignmentSubmissionView(
        String id,
        String userId,
        LocalDateTime submissionTime,
        String assignmentId,
        String content,
        Double score,
        String feedback,
        List<AssignmentSubmissionAttachmentView> attachments,
        Boolean aiAssisted
) {
}
