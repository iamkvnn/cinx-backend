package com.cinx.learning.community.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateReviewReplyRequest {
    @NotBlank
    @Schema(example = "Thank you for the wonderful review!")
    private String content;
}
