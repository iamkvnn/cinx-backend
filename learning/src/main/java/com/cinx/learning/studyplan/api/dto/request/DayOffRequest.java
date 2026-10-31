package com.cinx.learning.studyplan.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record DayOffRequest(
        @NotNull
        @Schema(example = "2026-09-30", description = "A day from today up to 60 days ahead (Asia/Ho_Chi_Minh)")
        LocalDate date
) {
}
