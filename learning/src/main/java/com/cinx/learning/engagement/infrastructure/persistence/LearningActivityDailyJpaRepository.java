package com.cinx.learning.engagement.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

interface LearningActivityDailyJpaRepository extends JpaRepository<LearningActivityDailyJpaEntity, String> {
    /** Per learner of one course: the last activity and the total active time. */
    @Query("""
        SELECT a.userId AS userId, MAX(a.lastActivityAt) AS lastActivityAt, COALESCE(SUM(a.activeSeconds), 0) AS activeSeconds
        FROM LearningActivityDaily a
        WHERE a.courseId = :courseId
        GROUP BY a.userId
    """)
    List<LearnerActivity> activityPerLearner(String courseId);

    interface LearnerActivity {
        String getUserId();
        java.time.LocalDateTime getLastActivityAt();
        Long getActiveSeconds();
    }

    Optional<LearningActivityDailyJpaEntity> findFirstByUserIdAndLastItemIdIsNotNullOrderByLastActivityAtDesc(String userId);

    Optional<LearningActivityDailyJpaEntity> findFirstByUserIdAndCourseIdAndLastItemIdIsNotNullOrderByLastActivityAtDesc(String userId, String courseId);

    /** Per course, the latest day row that names a lesson (the one "Học tiếp" resumes from). */
    @Query("SELECT a FROM LearningActivityDaily a " +
           "WHERE a.userId = :userId AND a.courseId IN :courseIds AND a.lastItemId IS NOT NULL " +
           "AND a.lastActivityAt = (SELECT MAX(b.lastActivityAt) FROM LearningActivityDaily b " +
           "WHERE b.userId = a.userId AND b.courseId = a.courseId AND b.lastItemId IS NOT NULL)")
    List<LearningActivityDailyJpaEntity> findLatestWithItemPerCourse(String userId, Collection<String> courseIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<LearningActivityDailyJpaEntity> findByUserIdAndCourseIdAndActivityDate(String userId, String courseId, LocalDate activityDate);

    @Query("SELECT COALESCE(SUM(a.activeSeconds), 0) FROM LearningActivityDaily a WHERE a.userId = :userId")
    Long sumActiveSecondsByUserId(String userId);

    @Query("SELECT a.userId FROM LearningActivityDaily a GROUP BY a.userId HAVING MAX(a.activityDate) BETWEEN :from AND :to")
    List<String> findUserIdsLastActiveBetween(LocalDate from, LocalDate to);

    @Query("SELECT MAX(a.activityDate) FROM LearningActivityDaily a WHERE a.userId = :userId")
    LocalDate findLastActivityDate(String userId);

    boolean existsByUserIdAndActivityDate(String userId, LocalDate activityDate);

    @Query("SELECT FUNCTION('DATE_FORMAT', a.activityDate, '%Y-%m'), COALESCE(SUM(a.activeSeconds), 0) " +
           "FROM LearningActivityDaily a " +
           "WHERE a.userId = :userId AND a.activityDate BETWEEN :startDate AND :endDate " +
           "GROUP BY FUNCTION('DATE_FORMAT', a.activityDate, '%Y-%m') " +
           "ORDER BY FUNCTION('DATE_FORMAT', a.activityDate, '%Y-%m') ASC")
    List<Object[]> aggregateUserActivityByMonth(String userId, LocalDate startDate, LocalDate endDate);

    @Query("SELECT FUNCTION('DATE_FORMAT', a.activityDate, '%Y-%m-%d'), COALESCE(SUM(a.activeSeconds), 0) " +
           "FROM LearningActivityDaily a " +
           "WHERE a.userId = :userId AND a.activityDate BETWEEN :startDate AND :endDate " +
           "GROUP BY FUNCTION('DATE_FORMAT', a.activityDate, '%Y-%m-%d') " +
           "ORDER BY FUNCTION('DATE_FORMAT', a.activityDate, '%Y-%m-%d') ASC")
    List<Object[]> aggregateUserActivityByDay(String userId, LocalDate startDate, LocalDate endDate);

    @Query("SELECT COALESCE(SUM(a.activeSeconds), 0) FROM LearningActivityDaily a " +
           "WHERE a.courseId = :courseId AND a.activityDate BETWEEN :startDate AND :endDate")
    Long sumActiveSecondsByCourseIdBetween(String courseId, LocalDate startDate, LocalDate endDate);

    @Query("SELECT COUNT(DISTINCT a.userId) FROM LearningActivityDaily a " +
           "WHERE a.courseId = :courseId AND a.activityDate BETWEEN :startDate AND :endDate AND a.activeSeconds > 0")
    Long countActiveLearnersByCourseIdBetween(String courseId, LocalDate startDate, LocalDate endDate);

    @Query("SELECT FUNCTION('DATE_FORMAT', a.activityDate, '%Y-%m'), COALESCE(SUM(a.activeSeconds), 0) " +
           "FROM LearningActivityDaily a " +
           "WHERE a.courseId = :courseId AND a.activityDate BETWEEN :startDate AND :endDate " +
           "GROUP BY FUNCTION('DATE_FORMAT', a.activityDate, '%Y-%m') " +
           "ORDER BY FUNCTION('DATE_FORMAT', a.activityDate, '%Y-%m') ASC")
    List<Object[]> aggregateCourseActivityByMonth(String courseId, LocalDate startDate, LocalDate endDate);

    @Query("SELECT FUNCTION('DATE_FORMAT', a.activityDate, '%Y-%m-%d'), COALESCE(SUM(a.activeSeconds), 0) " +
           "FROM LearningActivityDaily a " +
           "WHERE a.courseId = :courseId AND a.activityDate BETWEEN :startDate AND :endDate " +
           "GROUP BY FUNCTION('DATE_FORMAT', a.activityDate, '%Y-%m-%d') " +
           "ORDER BY FUNCTION('DATE_FORMAT', a.activityDate, '%Y-%m-%d') ASC")
    List<Object[]> aggregateCourseActivityByDay(String courseId, LocalDate startDate, LocalDate endDate);
}
