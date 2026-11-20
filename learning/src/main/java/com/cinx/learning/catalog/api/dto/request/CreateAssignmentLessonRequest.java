package com.cinx.learning.catalog.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CreateAssignmentLessonRequest {
        @NotBlank(message = "Description is required")
        @Schema(example = "Please submit your completed project via ZIP file.")
        private String description;
        
        @Valid
        private List<AttachmentDto> attachments;

        @Data
        public static class AttachmentDto {
                @NotBlank(message = "File key is required")
                @Schema(example = "assignments/spring-boot-project.zip")
                private String fileKey;
                
                @NotBlank(message = "File name is required")
                @Schema(example = "spring-boot-project.zip")
                private String fileName;
                
                @NotBlank(message = "File type is required")
                @Schema(example = "application/zip")
                private String fileType;
                
                @NotNull(message = "File size is required")
                @Min(value = 0, message = "File size must be non-negative")
                private Long fileSize;
        }
}

