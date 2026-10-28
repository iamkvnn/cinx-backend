package com.cinx.learning.catalog.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UpdateCoursePromoVideoRequest {
    @NotBlank
    @Schema(example = "courses/promo/12345-intro.mp4")
    private String fileKey;
    @NotBlank
    @Schema(example = "intro.mp4")
    private String fileName;
    @NotBlank
    @Pattern(regexp = "^video/.+", message = "File type must be a video")
    @Schema(example = "video/mp4")
    private String fileType;
    @NotNull
    @Min(1)
    @Schema(example = "52428800")
    private Long fileSize;
    @NotNull
    @Min(1)
    @Schema(example = "95", description = "Length in seconds")
    private Integer duration;
}
