package com.cinx.learning.assessment.infrastructure.persistence;

import com.cinx.learning.shared.domain.QuizSessionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

interface QuizSessionJpaRepository extends JpaRepository<QuizSessionJpaEntity, String> {

    /** One course's sessions in these states, with their submission (score is already on a 0–10 scale). */
    @Query("""
        SELECT qs FROM QuizSession qs LEFT JOIN FETCH qs.quizSessionSubmission
        WHERE qs.courseId = :courseId AND qs.status IN :statuses
    """)
    List<QuizSessionJpaEntity> findAllByCourseIdAndStatusIn(String courseId, java.util.Collection<QuizSessionStatus> statuses);

    /** Quiz sessions of these courses waiting for essay grading, per quiz lesson: count and the oldest submission. */
    @Query("""
        SELECT qs.courseId AS courseId, qs.quizLessonId AS quizLessonId, COUNT(qs) AS waiting, MIN(qs.endTime) AS oldest
        FROM QuizSession qs
        WHERE qs.courseId IN :courseIds AND qs.status = com.cinx.learning.shared.domain.QuizSessionStatus.PENDING_GRADE
        GROUP BY qs.courseId, qs.quizLessonId
    """)
    List<PendingGradeGroup> findPendingGradeGroups(java.util.Collection<String> courseIds);

    interface PendingGradeGroup {
        String getCourseId();
        String getQuizLessonId();
        long getWaiting();
        LocalDateTime getOldest();
    }

    /** Which of these quiz lessons the learner has submitted in any of the given statuses (prerequisite rule). */
    @Query("SELECT DISTINCT qs.quizLessonId FROM QuizSession qs WHERE qs.userId = :userId AND qs.quizLessonId IN :lessonIds AND qs.status IN :statuses")
    List<String> findLessonIdsWithStatus(@org.springframework.data.repository.query.Param("userId") String userId,
                                         @org.springframework.data.repository.query.Param("lessonIds") java.util.Collection<String> lessonIds,
                                         @org.springframework.data.repository.query.Param("statuses") java.util.Collection<QuizSessionStatus> statuses);

    @Query("""
        SELECT qs FROM QuizSession qs
        LEFT JOIN FETCH qs.quizSessionSubmission qss
        WHERE qs.quizLessonId = :quizLessonId
        AND (:userId IS NULL OR qs.userId = :userId)
    """)
    Page<QuizSessionJpaEntity> findAllByQuizLessonId(String quizLessonId, String userId, Pageable pageable);

    Integer countByQuizLessonIdAndUserId(String quizLessonId, String userId);

    boolean existsByQuizLessonIdAndUserIdAndStatus(String quizLessonId, String userId, QuizSessionStatus status);

    @Query("""
        SELECT qs FROM QuizSession qs
        WHERE qs.status = :status
        AND qs.endTime < :now
    """)
    List<QuizSessionJpaEntity> findExpiredSessions(QuizSessionStatus status, LocalDateTime now);
}

