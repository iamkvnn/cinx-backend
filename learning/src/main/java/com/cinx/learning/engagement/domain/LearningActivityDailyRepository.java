package com.cinx.learning.engagement.domain;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Port: where LearningActivityDaily rows are stored. */
public interface LearningActivityDailyRepository {
    /** Per learner of one course: the last activity and the total active time. */
    List<LearnerActivity> activityPerLearner(String courseId);

    Optional<LearningActivityDaily> findFirstByUserIdAndLastItemIdIsNotNullOrderByLastActivityAtDesc(String userId);

    Optional<LearningActivityDaily> findFirstByUserIdAndCourseIdAndLastItemIdIsNotNullOrderByLastActivityAtDesc(String userId, String courseId);

    /** Per course, the latest day row that names a lesson (the one "Học tiếp" resumes from). */
    List<LearningActivityDaily> findLatestWithItemPerCourse(String userId, Collection<String> courseIds);

    Optional<LearningActivityDaily> findByUserIdAndCourseIdAndActivityDate(String userId, String courseId, LocalDate activityDate);

    Long sumActiveSecondsByUserId(String userId);

    /** Learners whose latest day of activity falls between these days, both included. */
    List<String> findUserIdsLastActiveBetween(LocalDate from, LocalDate to);

    Optional<LocalDate> findLastActivityDate(String userId);

    boolean existsByUserIdAndActivityDate(String userId, LocalDate activityDate);

    List<Object[]> aggregateUserActivityByMonth(String userId, LocalDate startDate, LocalDate endDate);

    List<Object[]> aggregateUserActivityByDay(String userId, LocalDate startDate, LocalDate endDate);

    Long sumActiveSecondsByCourseIdBetween(String courseId, LocalDate startDate, LocalDate endDate);

    Long countActiveLearnersByCourseIdBetween(String courseId, LocalDate startDate, LocalDate endDate);

    List<Object[]> aggregateCourseActivityByMonth(String courseId, LocalDate startDate, LocalDate endDate);

    List<Object[]> aggregateCourseActivityByDay(String courseId, LocalDate startDate, LocalDate endDate);

    LearningActivityDaily save(LearningActivityDaily entity);

    interface LearnerActivity {
            String getUserId();
            java.time.LocalDateTime getLastActivityAt();
            Long getActiveSeconds();
        }
}
