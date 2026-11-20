package com.cinx.learning.engagement.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

/** The caller's own study time: everything so far, and each day of the current week (Monday first). */
public record LearningActivitySummaryResponse(
        @Schema(example = "136800")
        long totalSeconds,
        @Schema(example = "2026-09-28")
        LocalDate weekStart,
        List<Day> days
) {
    public record Day(
            @Schema(example = "2026-09-29")
            LocalDate date,
            @Schema(example = "2700")
            long seconds
    ) {
    }
}
