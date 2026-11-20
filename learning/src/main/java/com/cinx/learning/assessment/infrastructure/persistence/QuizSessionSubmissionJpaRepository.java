package com.cinx.learning.assessment.infrastructure.persistence;

import com.cinx.learning.shared.domain.QuizSessionStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

interface QuizSessionSubmissionJpaRepository extends JpaRepository<QuizSessionSubmissionJpaEntity, String> {
    Optional<QuizSessionSubmissionJpaEntity> findByQuizSessionId(String quizSessionId);

    @Query("""
            SELECT s FROM QuizSessionSubmission s
            JOIN s.quizSession qs
            WHERE qs.userId = :userId
              AND qs.quizLessonId = :quizLessonId
              AND qs.status IN (
                com.cinx.learning.shared.domain.QuizSessionStatus.SUBMITTED,
                com.cinx.learning.shared.domain.QuizSessionStatus.GRADED
              )
            ORDER BY s.submissionTime ASC
            """)
    List<QuizSessionSubmissionJpaEntity> findAllByUserIdAndQuizLessonId(
            @Param("userId") String userId,
            @Param("quizLessonId") String quizLessonId
    );

    @Query("""
            SELECT s.score FROM QuizSessionSubmission s
            JOIN s.quizSession qs
            WHERE qs.userId = :userId
              AND qs.quizLessonId = :quizLessonId
              AND qs.status IN (
                com.cinx.learning.shared.domain.QuizSessionStatus.SUBMITTED,
                com.cinx.learning.shared.domain.QuizSessionStatus.GRADED
              )
              AND s.score IS NOT NULL
            ORDER BY s.submissionTime ASC
            """)
    List<Double> findScoresByUserIdAndQuizLessonId(
            @Param("userId") String userId,
            @Param("quizLessonId") String quizLessonId
    );

    @Query("""
            SELECT s FROM QuizSessionSubmission s
            JOIN FETCH s.quizSession qs
            WHERE qs.quizLessonId = :quizLessonId
              AND qs.status IN (
                com.cinx.learning.shared.domain.QuizSessionStatus.SUBMITTED,
                com.cinx.learning.shared.domain.QuizSessionStatus.GRADED
              )
            ORDER BY s.submissionTime ASC
            """)
    List<QuizSessionSubmissionJpaEntity> findAllByQuizLessonId(String quizLessonId);

    List<QuizSessionSubmissionJpaEntity> findAllByQuizSessionIdIn(Collection<String> strings);
}
