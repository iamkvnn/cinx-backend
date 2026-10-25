package com.cinx.learning.catalog.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Port: where AssignmentLesson rows are stored. */
public interface AssignmentLessonRepository {
    Optional<AssignmentLesson> findByLessonId(String lessonId);

    java.util.List<AssignmentLesson> findAllByLessonIdIn(java.util.Collection<String> lessonIds);

    void delete(AssignmentLesson entity);

    Optional<AssignmentLesson> findById(String id);

    AssignmentLesson save(AssignmentLesson entity);
}
