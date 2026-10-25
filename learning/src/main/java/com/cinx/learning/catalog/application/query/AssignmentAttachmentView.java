package com.cinx.learning.catalog.application.query;

public record AssignmentAttachmentView(
        String id,
        String fileName,
        String fileType,
        Long fileSize,
        String attachmentUrl
) {
}

