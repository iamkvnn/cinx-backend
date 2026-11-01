package com.cinx.learning.engagement.application.mapper;

import com.cinx.learning.engagement.application.query.DailyGoalView;
import com.cinx.learning.engagement.domain.UserDailyGoal;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DailyGoalMapper {
    DailyGoalView toView(UserDailyGoal goal);
}
