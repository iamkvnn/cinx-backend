package com.cinx.learning.engagement.domain;

import com.cinx.learning.shared.domain.DailyGoalType;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import com.cinx.learning.shared.domain.Model;
import java.time.LocalDate;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class UserDailyGoal extends Model {
    private String userId;
    private DailyGoalType goalType;
    private String goalKey;
    private Integer targetValue;
    private Integer currentValue = 0;
    private String targetItemId;
    private LocalDate goalDate;
    private Boolean isCompleted = false;
}
