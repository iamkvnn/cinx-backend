package com.cinx.learning.community.domain;

import com.cinx.common.paging.PageQuery;
import com.cinx.common.paging.PageResult;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Port: where CourseAnswer rows are stored. */
public interface CourseAnswerRepository {
    PageResult<CourseAnswer> findByQuestionIdAndParentAnswerIdIsNullOrderByCreatedAtAsc(String questionId, PageQuery pageable);

    PageResult<CourseAnswer> findByParentAnswerIdOrderByCreatedAtAsc(String parentAnswerId, PageQuery pageable);

    List<CourseAnswer> findByQuestionId(String questionId);

    List<CourseAnswer> findByParentAnswerId(String parentAnswerId);

    void deleteByIdIn(Collection<String> answerIds);

    int countByQuestionId(String questionId);

    int countByParentAnswerId(String parentAnswerId);

    Long countAnswersByCourseIdBetween(String courseId, LocalDateTime start, LocalDateTime end);

    /** An instructor's own answers to questions of one lesson, newest first ("Dùng lại câu trả lời cũ"). */
    List<CourseAnswer> findInstructorAnswersInLesson(String lessonId, String userId, PageQuery pageable);

    List<CourseAnswer> findAllById(Iterable<String> ids);

    Optional<CourseAnswer> findById(String id);

    CourseAnswer save(CourseAnswer entity);
}
