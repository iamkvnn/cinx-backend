package com.cinx.learning.engagement.api.dto.request;

import com.cinx.learning.shared.domain.DailyGoalType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record SetDailyGoalRequest(
    @Schema(example = "WATCH_VIDEO")
    @NotNull(message = "goalType is required")
    DailyGoalType goalType,

    @Schema(example = "5")
    @Min(value = 1, message = "targetValue must be at least 1")
    Integer targetValue,
    
    @Schema(example = "2025-01-01")
    LocalDate goalDate,

    @Schema(example = "item_123")
    String targetItemId
) {}
