package com.cinx.learning.catalog.api.dto.response;

import com.cinx.learning.catalog.domain.VideoQuizQuestionType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record VideoQuestionResponse(
        @Schema(example = "vq_123")
        String id,
        @Schema(example = "What is the primary role of Spring Boot?")
        String questionText,
        @Schema(example = "SINGLE_CHOICE")
        VideoQuizQuestionType questionType,
        @Schema(example = "120")
        Integer timestampSeconds,
        List<VideoOptionResponse> options
) {
}
