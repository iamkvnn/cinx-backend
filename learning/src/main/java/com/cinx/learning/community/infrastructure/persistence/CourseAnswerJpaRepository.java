package com.cinx.learning.community.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.time.LocalDateTime;
import java.util.List;

@Repository
interface CourseAnswerJpaRepository extends JpaRepository<CourseAnswerJpaEntity, String> {
    Page<CourseAnswerJpaEntity> findByQuestionIdAndParentAnswerIdIsNullOrderByCreatedAtAsc(String questionId, Pageable pageable);
    Page<CourseAnswerJpaEntity> findByParentAnswerIdOrderByCreatedAtAsc(String parentAnswerId, Pageable pageable);
    List<CourseAnswerJpaEntity> findByQuestionId(String questionId);
    List<CourseAnswerJpaEntity> findByParentAnswerId(String parentAnswerId);
    void deleteByIdIn(Collection<String> answerIds);
    int countByQuestionId(String questionId);
    int countByParentAnswerId(String parentAnswerId);

    @Query("""
        SELECT COUNT(a)
        FROM CourseAnswer a
        JOIN CourseQuestion q ON a.questionId = q.id
        WHERE q.courseId = :courseId
            AND a.createdAt BETWEEN :start AND :end
    """)
    Long countAnswersByCourseIdBetween(String courseId, LocalDateTime start, LocalDateTime end);

    /** An instructor's own answers to questions of one lesson, newest first ("Dùng lại câu trả lời cũ"). */
    @Query("""
        SELECT a
        FROM CourseAnswer a
        JOIN CourseQuestion q ON a.questionId = q.id
        WHERE q.lessonId = :lessonId
            AND a.userId = :userId
            AND a.isInstructorAnswer = true
        ORDER BY a.createdAt DESC
    """)
    List<CourseAnswerJpaEntity> findInstructorAnswersInLesson(String lessonId, String userId, org.springframework.data.domain.Pageable pageable);
}
