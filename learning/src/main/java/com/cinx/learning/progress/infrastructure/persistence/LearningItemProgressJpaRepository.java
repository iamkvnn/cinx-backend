package com.cinx.learning.progress.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

interface LearningItemProgressJpaRepository extends JpaRepository<LearningItemProgressJpaEntity, String> {
    /** Learners who completed each item of one course (rows are pre-created at enrolment, so only is_completed counts). */
    @Query("""
        SELECT p.itemId AS itemId, COUNT(p) AS total
        FROM LearningItemProgress p
        WHERE p.courseProgress.courseId = :courseId AND p.isCompleted = TRUE
        GROUP BY p.itemId
    """)
    List<ItemCount> completedPerItem(String courseId);

    /** Who passed these items of one course. */
    @Query("""
        SELECT p.itemId AS itemId, p.courseProgress.userId AS userId
        FROM LearningItemProgress p
        WHERE p.courseProgress.courseId = :courseId AND p.itemId IN :itemIds AND p.isPassed = TRUE
    """)
    List<PassedRow> passedIn(String courseId, java.util.Collection<String> itemIds);

    interface ItemCount {
        String getItemId();
        long getTotal();
    }

    interface PassedRow {
        String getItemId();
        String getUserId();
    }

    @Query("SELECT lip FROM LearningItemProgress lip " +
            "JOIN lip.courseProgress cp " +
            "WHERE cp.userId = :userId AND cp.courseId = :courseId")
    List<LearningItemProgressJpaEntity> findAllByUserIdAndCourseId(String userId, String courseId);

    @Query("SELECT lip FROM LearningItemProgress lip " +
            "JOIN lip.courseProgress cp " +
            "WHERE cp.userId = :userId AND lip.itemId = :itemId")
    Optional<LearningItemProgressJpaEntity> findByItemIdAndUserId(String itemId, String userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT lip FROM LearningItemProgress lip " +
            "JOIN lip.courseProgress cp " +
            "WHERE cp.userId = :userId AND lip.itemId = :itemId")
    Optional<LearningItemProgressJpaEntity> findByItemIdAndUserIdForUpdate(String itemId, String userId);

    @Query("SELECT lip FROM LearningItemProgress lip " +
            "JOIN lip.courseProgress cp " +
            "WHERE cp.userId = :userId AND lip.itemId IN :itemIds")
    List<LearningItemProgressJpaEntity> findAllByUserIdAndItemIdIn(String userId, List<String> itemIds);
}
