package com.cinx.learning.assessment.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record AssignmentSubmissionAttachmentResponse(
        @Schema(example = "att_123")
        String id,
        @Schema(example = "homework.pdf")
        String fileName,
        @Schema(example = "application/pdf")
        String fileType,
        @Schema(example = "2048576")
        long fileSize,
        @Schema(example = "https://example.com/homework.pdf")
        String attachmentUrl
) {
}
