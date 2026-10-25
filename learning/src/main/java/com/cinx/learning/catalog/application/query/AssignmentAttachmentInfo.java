package com.cinx.learning.catalog.application.query;

import lombok.Builder;
import lombok.Value;
import com.cinx.learning.catalog.domain.AssignmentAttachment;

/** What other features may read of a AssignmentAttachment; a copy, so the domain class stays private to catalog. */
@Value
@Builder
public class AssignmentAttachmentInfo {
    String id;
    Long version;
    String attachmentUrl;
    String fileKey;
    String fileName;
    String fileType;
    Long fileSize;
    String assignmentLessonId;

    public static AssignmentAttachmentInfo of(AssignmentAttachment domain) {
        return AssignmentAttachmentInfo.builder()
                .id(domain.getId())
                .version(domain.getVersion())
                .attachmentUrl(domain.getAttachmentUrl())
                .fileKey(domain.getFileKey())
                .fileName(domain.getFileName())
                .fileType(domain.getFileType())
                .fileSize(domain.getFileSize())
                .assignmentLessonId(domain.getAssignmentLessonId())
                .build();
    }
}
