package com.cinx.learning.community.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
interface CourseQuestionJpaRepository extends JpaRepository<CourseQuestionJpaEntity, String> {
    @Query("""
        SELECT q.lessonId AS lessonId, COUNT(q) AS total
        FROM CourseQuestion q
        WHERE q.courseId = :courseId AND q.lessonId IS NOT NULL
        GROUP BY q.lessonId
    """)
    List<LessonCount> countPerLesson(String courseId);

    interface LessonCount {
        String getLessonId();
        long getTotal();
    }

    Page<CourseQuestionJpaEntity> findByCourseId(String courseId, Pageable pageable);
    Page<CourseQuestionJpaEntity> findByCourseIdAndLessonId(String courseId, String lessonId, Pageable pageable);

    @Query("""
        SELECT q
        FROM CourseQuestion q
        WHERE q.courseId = :courseId
          AND (:lessonId IS NULL OR q.lessonId = :lessonId)
          AND (:query IS NULL OR q.title LIKE %:query% OR q.content LIKE %:query% OR q.userId LIKE %:query%)
          AND (:unanswered = false OR NOT EXISTS (
                SELECT a.id FROM CourseAnswer a WHERE a.questionId = q.id AND a.isInstructorAnswer = true))
    """)
    Page<CourseQuestionJpaEntity> search(String courseId, String lessonId, String query, boolean unanswered, Pageable pageable);

    /** Full-text matches of a course without a report; the lesson's own first, then answered, then the closest. */
    @Query(value = """
        SELECT q.* FROM course_questions q
        WHERE q.course_id = :courseId
          AND MATCH(q.title, q.content) AGAINST (:terms IN NATURAL LANGUAGE MODE) >= :minScore
          AND NOT EXISTS (SELECT 1 FROM reports r WHERE r.ref_id = q.id AND r.type = 'QUESTION')
        ORDER BY (q.lesson_id = :lessonId) DESC,
                 EXISTS (SELECT 1 FROM course_answers a WHERE a.question_id = q.id) DESC,
                 MATCH(q.title, q.content) AGAINST (:terms IN NATURAL LANGUAGE MODE) DESC
        LIMIT :limit
    """, nativeQuery = true)
    List<CourseQuestionJpaEntity> findSimilar(String courseId, String lessonId, String terms, double minScore, int limit);

    /** A lesson's questions no instructor has answered yet, most shared first. */
    @Query("""
        SELECT q FROM CourseQuestion q
        WHERE q.lessonId = :lessonId
          AND NOT EXISTS (SELECT a.id FROM CourseAnswer a WHERE a.questionId = q.id AND a.isInstructorAnswer = true)
        ORDER BY q.upvoteCount DESC, q.createdAt ASC
    """)
    List<CourseQuestionJpaEntity> findUnansweredByLessonId(String lessonId);

    /** Unanswered questions of these courses per lesson: count, shared upvotes and the oldest question. */
    @Query("""
        SELECT q.courseId AS courseId, q.lessonId AS lessonId, COUNT(q) AS waiting,
               COALESCE(SUM(q.upvoteCount), 0) AS upvotes, MIN(q.createdAt) AS oldest
        FROM CourseQuestion q
        WHERE q.courseId IN :courseIds
          AND NOT EXISTS (SELECT a.id FROM CourseAnswer a WHERE a.questionId = q.id AND a.isInstructorAnswer = true)
        GROUP BY q.courseId, q.lessonId
    """)
    List<UnansweredGroup> findUnansweredGroups(java.util.Collection<String> courseIds);

    interface UnansweredGroup {
        String getCourseId();
        String getLessonId();
        long getWaiting();
        long getUpvotes();
        LocalDateTime getOldest();
    }

    @Query("""
        SELECT COUNT(q)
        FROM CourseQuestion q
        WHERE q.courseId = :courseId
            AND q.createdAt BETWEEN :start AND :end
    """)
    Long countQuestionsByCourseIdBetween(String courseId, LocalDateTime start, LocalDateTime end);

    @Query("""
        SELECT COUNT(DISTINCT q.id)
        FROM CourseQuestion q
        JOIN CourseAnswer a ON a.questionId = q.id
        WHERE q.courseId = :courseId
            AND q.createdAt BETWEEN :start AND :end
            AND a.isInstructorAnswer = true
    """)
    Long countQuestionsWithInstructorAnswerBetween(String courseId, LocalDateTime start, LocalDateTime end);

    @Query("""
        SELECT FUNCTION('DATE_FORMAT', q.createdAt, '%Y-%m-%d'), COUNT(q)
        FROM CourseQuestion q
        WHERE q.courseId = :courseId
            AND q.createdAt BETWEEN :start AND :end
        GROUP BY FUNCTION('DATE_FORMAT', q.createdAt, '%Y-%m-%d')
        ORDER BY FUNCTION('DATE_FORMAT', q.createdAt, '%Y-%m-%d') ASC
    """)
    List<Object[]> aggregateQuestionsByDay(String courseId, LocalDateTime start, LocalDateTime end);

    @Query("""
        SELECT FUNCTION('DATE_FORMAT', q.createdAt, '%Y-%m'), COUNT(q)
        FROM CourseQuestion q
        WHERE q.courseId = :courseId
            AND q.createdAt BETWEEN :start AND :end
        GROUP BY FUNCTION('DATE_FORMAT', q.createdAt, '%Y-%m')
        ORDER BY FUNCTION('DATE_FORMAT', q.createdAt, '%Y-%m') ASC
    """)
    List<Object[]> aggregateQuestionsByMonth(String courseId, LocalDateTime start, LocalDateTime end);
}
