package com.cinx.learning.catalog.application.command;

import lombok.Data;

import java.util.List;

@Data
public class UpdateAssignmentLessonCommand {
    private String description;

    private List<AttachmentDto> attachments;

    @Data
    public static class AttachmentDto {
        private String fileKey;
        
        private String fileName;
        
        private String fileType;
        
        private Long fileSize;
    }
}

