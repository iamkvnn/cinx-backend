package com.cinx.learning.engagement.application;

import com.cinx.learning.engagement.application.query.LearningActivityDailyInfo;
import lombok.Value;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** What the other features read of a learner's study activity. */
public interface EngagementLookup {
    /** Per course of these, the latest day that names a lesson: the one "Hoc tiep" resumes from. */
    List<LearningActivityDailyInfo> latestActivityWithLessonPerCourse(String userId, Collection<String> courseIds);

    Optional<LearningActivityDailyInfo> lastActivityWithLesson(String userId);

    Optional<LearningActivityDailyInfo> lastActivityWithLesson(String userId, String courseId);

    /** Per learner of a course, the last activity and the total active time. */
    List<LearnerActivity> activityPerLearner(String courseId);

    /** Learners with a streak of at least {@code minStreak} days that today has not extended yet. */
    List<String> userIdsWithStreakAtRisk(LocalDate today, int minStreak);

    /** The learner's current streak, 0 when there is none. */
    int currentStreak(String userId);

    /** Learners with a daily goal for this day that is not completed. */
    List<String> userIdsWithPendingGoals(LocalDate day);

    List<PendingGoal> pendingGoals(String userId, LocalDate day);

    /** Learners whose latest day of activity falls between these days, both included. */
    List<String> userIdsLastActiveBetween(LocalDate from, LocalDate to);

    Optional<LocalDate> lastActiveDay(String userId);

    boolean activeOn(String userId, LocalDate day);

    @Value
    class PendingGoal {
        String goalType;
        int targetValue;
        int currentValue;
    }

    @Value
    class LearnerActivity {
        String userId;
        LocalDateTime lastActivityAt;
        Long activeSeconds;
    }
}
