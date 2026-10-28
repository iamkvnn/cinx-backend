package com.cinx.learning.catalog.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateArticleLessonRequest {
    @NotBlank
    @Schema(example = "courses/articles/12345-spring-boot.pdf")
    private String fileKey;
    @NotBlank
    @Schema(example = "spring-boot-intro.pdf")
    private String fileName;
    @NotBlank
    @Schema(example = "application/pdf")
    private String fileType;
    @NotNull
    @Min(1)
    @Schema(example = "1048576")
    private Long fileSize;
}

