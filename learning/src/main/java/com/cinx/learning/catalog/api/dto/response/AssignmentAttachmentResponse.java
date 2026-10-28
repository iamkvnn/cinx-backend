package com.cinx.learning.catalog.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record AssignmentAttachmentResponse(
        @Schema(example = "att_123")
        String id,
        @Schema(example = "spring-boot-project.zip")
        String fileName,
        @Schema(example = "application/zip")
        String fileType,
        @Schema(example = "1024000")
        Long fileSize,
        @Schema(example = "https://example.com/downloads/spring-boot-project.zip")
        String attachmentUrl
) {
}

