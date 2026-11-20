package com.cinx.learning.engagement.application.command;

import com.cinx.learning.shared.domain.DailyGoalType;

import java.time.LocalDate;

public record SetDailyGoalCommand(
    DailyGoalType goalType,

    Integer targetValue,
    
    LocalDate goalDate,

    String targetItemId
) {}
