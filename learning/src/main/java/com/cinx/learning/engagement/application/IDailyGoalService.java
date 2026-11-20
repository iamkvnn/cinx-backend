package com.cinx.learning.engagement.application;

import com.cinx.learning.shared.domain.DailyGoalType;
import com.cinx.learning.engagement.application.command.SetDailyGoalCommand;
import com.cinx.learning.engagement.application.query.DailyGoalView;

import java.time.LocalDate;
import java.util.List;

public interface IDailyGoalService {
    List<DailyGoalView> getUserDailyGoals(String userId, LocalDate date);
    DailyGoalView setDailyGoal(String userId, SetDailyGoalCommand request);
    void recordProgress(String userId, DailyGoalType goalType, Integer amount);
    void recordLessonCompleted(String userId, String itemId);
    void deleteDailyGoal(String userId, LocalDate date, DailyGoalType goalType, String targetItemId);
    List<DailyGoalView> getUserDailyGoalsInMonth(String userId, int year, int month);
}
