package com.cinx.learning.assessment.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.Valid;

import java.util.List;

public record CreateAssignmentSubmissionRequest(
    @Schema(example = "Here is my submission content.")
    @NotBlank(message = "content must not be blank")
    String content,

    @NotEmpty(message = "attachments must not be empty") @Valid
    List<AttachmentRequest> attachments
) {
    public record AttachmentRequest(
        @Schema(example = "file_key_123")
        @NotBlank(message = "fileKey must not be blank")
        String fileKey,

        @Schema(example = "submission.pdf")
        @NotBlank(message = "fileName must not be blank")
        String fileName,

        @Schema(example = "application/pdf")
        @NotBlank(message = "fileType must not be blank")
        String fileType,

        @Schema(example = "102400")
        @NotNull(message = "fileSize must not be null")
        @Min(value = 1, message = "fileSize must be non-negative")
        Long fileSize
    ){}
}
