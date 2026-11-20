package com.cinx.learning.catalog.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateCourseImageRequest {
    @NotBlank
    @Schema(example = "courses/images/spring-boot-cover-v2.jpg")
    private String fileKey;
}

