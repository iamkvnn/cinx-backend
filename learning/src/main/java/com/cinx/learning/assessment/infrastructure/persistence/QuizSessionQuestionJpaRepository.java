package com.cinx.learning.assessment.infrastructure.persistence;

import com.cinx.learning.shared.domain.QuizQuestionType;
import com.cinx.learning.shared.domain.QuizSessionStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.*;

interface QuizSessionQuestionJpaRepository extends JpaRepository<QuizSessionQuestionJpaEntity, String> {
    Page<QuizSessionQuestionJpaEntity> findAllByQuizSessionId(String quizSessionId, Pageable pageable);

    Optional<QuizSessionQuestionJpaEntity> findByQuizSessionIdAndQuestionId(String quizSessionId, String questionId);

    @Query("SELECT q.questionId as questionId, COUNT(q) as totalAttempts, " +
           "SUM(CASE WHEN q.score >= 1.0 THEN 1 ELSE 0 END) as correctAttempts " +
           "FROM QuizSessionQuestion q " +
           "JOIN q.quizSession s " +
           "WHERE s.quizLessonId = :quizId " +
           "AND s.status IN (com.cinx.learning.shared.domain.QuizSessionStatus.SUBMITTED, com.cinx.learning.shared.domain.QuizSessionStatus.GRADED) " +
           "GROUP BY q.questionId")
    List<Object[]> getQuizAnalyticsByQuizId(String quizId);

    @Query("""
            SELECT q FROM QuizSessionQuestion q
            JOIN FETCH q.quizSession s
            WHERE q.questionId IN :questionIds
              AND s.quizLessonId = :quizLessonId
            """)
    List<QuizSessionQuestionJpaEntity> findAllByQuizLessonIdAndQuestionIdIn(
            String quizLessonId,
            List<String> questionIds
    );

    @Query("""
            SELECT q FROM QuizSessionQuestion q
            WHERE q.quizSessionId = :sessionId
              AND q.questionType = com.cinx.learning.shared.domain.QuizQuestionType.ESSAY
            """)
    List<QuizSessionQuestionJpaEntity> findAllEssayByQuizSessionId(String sessionId);

    List<QuizSessionQuestionJpaEntity> findAllByQuizSessionIdIn(Collection<String> strings);
}
