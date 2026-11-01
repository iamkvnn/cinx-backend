package com.cinx.learning.dailyquiz.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

public record DailyQuizResponse(
        @Schema(example = "quiz_123")
        String id,
        @Schema(example = "BACKEND")
        String topic,
        @Schema(example = "Không tìm thấy đơn hàng, API nên trả về mã nào?")
        String prompt,
        String codeSnippet,
        List<Option> options,
        @Schema(description = "Public on purpose: this is a teaser, the answer is revealed client-side", example = "C")
        String answerKey,
        String explanation,
        @Schema(description = "Admin views only", example = "true")
        Boolean active,
        @Schema(description = "Admin views only", example = "0")
        Integer sortOrder,
        @Schema(description = "The day this question is served for (Asia/Ho_Chi_Minh)", example = "2026-09-29")
        LocalDate day
) {
    public record Option(
            @Schema(example = "A")
            String key,
            @Schema(example = "200 OK")
            String text
    ) {
    }
}
