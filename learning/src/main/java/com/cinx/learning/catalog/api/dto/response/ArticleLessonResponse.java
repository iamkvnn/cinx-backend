package com.cinx.learning.catalog.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record ArticleLessonResponse (
        @Schema(example = "https://example.com/courses/articles/12345-spring-boot.pdf")
        String articleUrl,
        @Schema(example = "spring-boot-intro.pdf")
        String fileName,
        @Schema(example = "application/pdf")
        String fileType,
        @Schema(example = "1048576")
        Long fileSize
){
}

