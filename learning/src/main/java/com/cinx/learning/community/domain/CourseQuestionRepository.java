package com.cinx.learning.community.domain;

import com.cinx.common.paging.PageQuery;
import com.cinx.common.paging.PageResult;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Port: where CourseQuestion rows are stored. */
public interface CourseQuestionRepository {
    List<LessonCount> countPerLesson(String courseId);

    PageResult<CourseQuestion> search(String courseId, String lessonId, String query, boolean unanswered, PageQuery pageable);

    /** Questions of a course matching these words, without a report; the lesson's own first, then answered ones. */
    List<CourseQuestion> findSimilar(String courseId, String lessonId, String terms, double minScore, int limit);

    /** A lesson's questions no instructor has answered yet, most shared first. */
    List<CourseQuestion> findUnansweredByLessonId(String lessonId);

    /** Unanswered questions of these courses per lesson: count, shared upvotes and the oldest question. */
    List<UnansweredGroup> findUnansweredGroups(java.util.Collection<String> courseIds);

    Long countQuestionsByCourseIdBetween(String courseId, LocalDateTime start, LocalDateTime end);

    Long countQuestionsWithInstructorAnswerBetween(String courseId, LocalDateTime start, LocalDateTime end);

    List<Object[]> aggregateQuestionsByDay(String courseId, LocalDateTime start, LocalDateTime end);

    List<Object[]> aggregateQuestionsByMonth(String courseId, LocalDateTime start, LocalDateTime end);

    void delete(CourseQuestion entity);

    List<CourseQuestion> findAllById(Iterable<String> ids);

    Optional<CourseQuestion> findById(String id);

    CourseQuestion save(CourseQuestion entity);

    interface LessonCount {
            String getLessonId();
            long getTotal();
        }

    interface UnansweredGroup {
            String getCourseId();
            String getLessonId();
            long getWaiting();
            long getUpvotes();
            LocalDateTime getOldest();
        }
}
