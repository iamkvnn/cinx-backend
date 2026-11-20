package com.cinx.learning.catalog.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateVideoLessonRequest {
    @NotBlank
    @Schema(example = "videos/12345-spring-boot.mp4")
    private String fileKey;
    @NotBlank
    @Schema(example = "spring-boot-intro.mp4")
    private String fileName;
    @NotBlank
    @Schema(example = "video/mp4")
    private String fileType;
    @NotNull
    @Min(1)
    @Schema(example = "104857600")
    private Long fileSize;
    @Min(0)
    @Schema(example = "600")
    private Integer duration;
}

