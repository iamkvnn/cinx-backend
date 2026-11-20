package com.cinx.learning.catalog.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class UpdateAssignmentLessonRequest {
    @Schema(example = "Updated description for the assignment.")
    private String description;

    @Valid
    private List<AttachmentDto> attachments;

    @Data
    public static class AttachmentDto {
        @NotBlank(message = "File key is required")
        @Schema(example = "assignments/spring-boot-project-v2.zip")
        private String fileKey;
        
        @NotBlank(message = "File name is required")
        @Schema(example = "spring-boot-project-v2.zip")
        private String fileName;
        
        @NotBlank(message = "File type is required")
        @Schema(example = "application/zip")
        private String fileType;
        
        @NotNull(message = "File size is required")
        @Schema(example = "2048000")
        private Long fileSize;
    }
}

