package com.cinx.learning.catalog.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.List;

@Data
public class CreateCourseImageRequest {
    @NotEmpty(message = "Images list cannot be empty")
    @Valid
    private List<ImageDto> images;

    @Data
    public static class ImageDto {
        @NotBlank(message = "File key is required")
        @Schema(example = "courses/images/spring-boot-cover.jpg")
        private String fileKey;
    }
}

