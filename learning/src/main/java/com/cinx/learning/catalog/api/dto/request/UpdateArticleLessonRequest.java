package com.cinx.learning.catalog.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateArticleLessonRequest {
    @NotBlank
    @Schema(example = "courses/articles/12345-spring-boot-v2.pdf")
    private String fileKey;
    @NotBlank
    @Schema(example = "spring-boot-intro-v2.pdf")
    private String fileName;
    @NotBlank
    @Schema(example = "application/pdf")
    private String fileType;
    @Min(1)
    @Schema(example = "1048576")
    private Long fileSize;
}

