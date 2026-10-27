package com.cinx.learning.engagement.application.query;

import com.cinx.learning.shared.domain.DailyGoalType;

import java.time.LocalDate;

public record DailyGoalView(
        String id,
        String userId,
        DailyGoalType goalType,
        Integer targetValue,
        Integer currentValue,
        LocalDate goalDate,
        String targetItemId,
        Boolean isCompleted
) {
}
