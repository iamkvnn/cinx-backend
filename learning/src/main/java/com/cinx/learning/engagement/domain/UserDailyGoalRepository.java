package com.cinx.learning.engagement.domain;

import com.cinx.learning.shared.domain.DailyGoalType;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Port: where UserDailyGoal rows are stored. */
public interface UserDailyGoalRepository {
    List<UserDailyGoal> findByUserIdAndGoalDate(String userId, LocalDate goalDate);

    Optional<UserDailyGoal> findByUserIdAndGoalDateAndGoalKey(String userId, LocalDate goalDate, String goalKey);

    Optional<UserDailyGoal> findByUserIdAndGoalDateAndGoalTypeAndTargetItemId(String userId, LocalDate goalDate, DailyGoalType goalType, String targetItemId);

    List<UserDailyGoal> findByUserIdAndGoalDateBetween(String userId, LocalDate startDate, LocalDate endDate);

    List<UserDailyGoal> findByGoalDateAndIsCompletedFalse(LocalDate goalDate);

    void delete(UserDailyGoal entity);

    UserDailyGoal save(UserDailyGoal entity);
}
