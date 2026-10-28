package com.cinx.learning.catalog.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

interface QuizQuestionJpaRepository extends JpaRepository<QuizQuestionJpaEntity, String> {
    @Query("SELECT q FROM QuizQuestion q LEFT JOIN FETCH q.options WHERE q.quizLesson.lessonId = :quizLessonId")
    List<QuizQuestionJpaEntity> findAllByQuizLessonId(String quizLessonId);

    @Query("SELECT q FROM QuizQuestion q LEFT JOIN FETCH q.options WHERE q.quizLesson.lessonId = :quizLessonId AND q.needSync = true")
    List<QuizQuestionJpaEntity> findAllByQuizLessonIdAndNeedSync(String quizLessonId);

    @Query("SELECT q FROM QuizQuestion q LEFT JOIN FETCH q.options WHERE q.id = :id AND q.quizLesson.lessonId = :quizLessonId")
    Optional<QuizQuestionJpaEntity> findByIdAndQuizLessonId(String id, String quizLessonId);

    @Query("SELECT COUNT(q) FROM QuizQuestion q WHERE q.quizLesson.lessonId = :quizLessonId")
    int countByQuizLessonId(String quizLessonId);

    @Query("""
        SELECT q.quizLesson.lessonId AS lessonId, COUNT(q) AS total
        FROM QuizQuestion q
        WHERE q.quizLesson.lessonId IN :quizLessonIds
        GROUP BY q.quizLesson.lessonId
    """)
    List<QuizCount> countPerQuizLesson(java.util.Collection<String> quizLessonIds);

    interface QuizCount {
        String getLessonId();
        long getTotal();
    }
}

