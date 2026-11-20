package com.cinx.learning.engagement.application;

import com.cinx.learning.engagement.application.query.LearningActivityDailyInfo;
import com.cinx.learning.engagement.domain.LearningActivityDailyRepository;
import com.cinx.learning.engagement.domain.UserDailyGoal;
import com.cinx.learning.engagement.domain.UserDailyGoalRepository;
import com.cinx.learning.engagement.domain.UserStreak;
import com.cinx.learning.engagement.domain.UserStreakRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EngagementLookupService implements EngagementLookup {
    private final LearningActivityDailyRepository activityRepository;
    private final UserStreakRepository streakRepository;
    private final UserDailyGoalRepository goalRepository;

    @Override
    public List<LearningActivityDailyInfo> latestActivityWithLessonPerCourse(String userId, Collection<String> courseIds) {
        return activityRepository.findLatestWithItemPerCourse(userId, courseIds).stream().map(LearningActivityDailyInfo::of).toList();
    }

    @Override
    public Optional<LearningActivityDailyInfo> lastActivityWithLesson(String userId) {
        return activityRepository.findFirstByUserIdAndLastItemIdIsNotNullOrderByLastActivityAtDesc(userId).map(LearningActivityDailyInfo::of);
    }

    @Override
    public Optional<LearningActivityDailyInfo> lastActivityWithLesson(String userId, String courseId) {
        return activityRepository.findFirstByUserIdAndCourseIdAndLastItemIdIsNotNullOrderByLastActivityAtDesc(userId, courseId)
                .map(LearningActivityDailyInfo::of);
    }

    @Override
    public List<LearnerActivity> activityPerLearner(String courseId) {
        return activityRepository.activityPerLearner(courseId).stream()
                .map(row -> new LearnerActivity(row.getUserId(), row.getLastActivityAt(), row.getActiveSeconds()))
                .toList();
    }

    @Override
    public List<String> userIdsWithStreakAtRisk(LocalDate today, int minStreak) {
        return streakRepository.findUserIdsWithStreakLastExtendedOn(minStreak, today.minusDays(1));
    }

    @Override
    public int currentStreak(String userId) {
        return streakRepository.findByUserId(userId).map(UserStreak::getCurrentStreak).orElse(0);
    }

    @Override
    public List<String> userIdsWithPendingGoals(LocalDate day) {
        return goalRepository.findByGoalDateAndIsCompletedFalse(day).stream().map(UserDailyGoal::getUserId).distinct().toList();
    }

    @Override
    public List<PendingGoal> pendingGoals(String userId, LocalDate day) {
        return goalRepository.findByUserIdAndGoalDate(userId, day).stream()
                .filter(goal -> !Boolean.TRUE.equals(goal.getIsCompleted()))
                .map(goal -> new PendingGoal(goal.getGoalType().name(), value(goal.getTargetValue()), value(goal.getCurrentValue())))
                .toList();
    }

    @Override
    public List<String> userIdsLastActiveBetween(LocalDate from, LocalDate to) {
        return activityRepository.findUserIdsLastActiveBetween(from, to);
    }

    @Override
    public Optional<LocalDate> lastActiveDay(String userId) {
        return activityRepository.findLastActivityDate(userId);
    }

    @Override
    public boolean activeOn(String userId, LocalDate day) {
        return activityRepository.existsByUserIdAndActivityDate(userId, day);
    }

    private static int value(Integer value) {
        return value == null ? 0 : value;
    }
}
