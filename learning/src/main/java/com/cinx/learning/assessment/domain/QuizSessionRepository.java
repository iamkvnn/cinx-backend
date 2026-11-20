package com.cinx.learning.assessment.domain;

import com.cinx.learning.shared.domain.QuizSessionStatus;

import com.cinx.common.paging.PageQuery;
import com.cinx.common.paging.PageResult;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Port: where QuizSession rows are stored. */
public interface QuizSessionRepository {
    /** One course's sessions in these states, with their submission (score is already on a 0–10 scale). */
    List<QuizSession> findAllByCourseIdAndStatusIn(String courseId, java.util.Collection<QuizSessionStatus> statuses);

    /** Quiz sessions of these courses waiting for essay grading, per quiz lesson: count and the oldest submission. */
    List<PendingGradeGroup> findPendingGradeGroups(java.util.Collection<String> courseIds);

    /** Which of these quiz lessons the learner has submitted in any of the given statuses (prerequisite rule). */
    List<String> findLessonIdsWithStatus(String userId, java.util.Collection<String> lessonIds, java.util.Collection<QuizSessionStatus> statuses);

    PageResult<QuizSession> findAllByQuizLessonId(String quizLessonId, String userId, PageQuery pageable);

    Integer countByQuizLessonIdAndUserId(String quizLessonId, String userId);

    boolean existsByQuizLessonIdAndUserIdAndStatus(String quizLessonId, String userId, QuizSessionStatus status);

    List<QuizSession> findExpiredSessions(QuizSessionStatus status, LocalDateTime now);

    Optional<QuizSession> findById(String id);

    QuizSession save(QuizSession entity);

    interface PendingGradeGroup {
            String getCourseId();
            String getQuizLessonId();
            long getWaiting();
            LocalDateTime getOldest();
        }

    List<QuizSession> findAllById(Iterable<String> ids);
}
