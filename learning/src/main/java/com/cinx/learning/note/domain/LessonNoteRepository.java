package com.cinx.learning.note.domain;

import java.util.List;
import java.util.Optional;

/** Port for the stored LessonNote rows. */
public interface LessonNoteRepository {
    List<LessonNote> findByUserIdAndCourseIdAndLessonIdOrderByCreatedAtDesc(String userId, String courseId, String lessonId);

    List<LessonNote> findByUserIdAndCourseIdOrderByCreatedAtDesc(String userId, String courseId);

    void delete(LessonNote entity);

    Optional<LessonNote> findById(String id);

    LessonNote save(LessonNote entity);
}
