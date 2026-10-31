package com.cinx.learning.assessment.api.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LineCommentRequest(
        @NotBlank String attachmentId,
        @NotNull @Min(1) Integer line,
        @NotBlank @Size(max = 2000) String content
) {
}
