package com.cinx.learning.assessment.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

public record AssignmentSubmissionResponse(
        @Schema(example = "sub_123")
        String id,
        @Schema(example = "user_123")
        String userId,
        @Schema(example = "2025-01-01T10:00:00")
        LocalDateTime submissionTime,
        @Schema(example = "assign_123")
        String assignmentId,
        @Schema(example = "Here is my assignment submission.")
        String content,
        @Schema(example = "95.5")
        Double score,
        @Schema(example = "Great work!")
        String feedback,
        List<AssignmentSubmissionAttachmentResponse> attachments,
        @Schema(description = "The instructor graded with an AI suggestion and set the final grade")
        Boolean aiAssisted
) {
}
