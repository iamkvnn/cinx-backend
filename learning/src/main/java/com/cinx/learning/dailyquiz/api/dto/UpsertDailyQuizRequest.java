package com.cinx.learning.dailyquiz.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpsertDailyQuizRequest(
        @NotBlank @Size(max = 60)
        @Schema(example = "BACKEND")
        String topic,

        @NotBlank @Size(max = 500)
        @Schema(example = "Không tìm thấy đơn hàng, API nên trả về mã nào?")
        String prompt,

        @Size(max = 4000)
        @Schema(example = "@GetMapping(\"/orders/{id}\")\nOrder get(@PathVariable Long id) { ... }")
        String codeSnippet,

        @NotNull @Size(min = 4, max = 4, message = "A daily quiz has exactly 4 options")
        List<@Valid Option> options,

        @NotBlank @Pattern(regexp = "[A-D]", message = "Answer key must be A, B, C or D")
        @Schema(example = "C")
        String answerKey,

        @NotBlank @Size(max = 1000)
        @Schema(example = "404 Not Found: tài nguyên được yêu cầu không tồn tại.")
        String explanation,

        @Schema(example = "true")
        Boolean active
) {
    public record Option(
            @NotBlank @Pattern(regexp = "[A-D]", message = "Option key must be A, B, C or D")
            @Schema(example = "A")
            String key,

            @NotBlank @Size(max = 200)
            @Schema(example = "200 OK")
            String text
    ) {
    }
}
