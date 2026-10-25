package com.cinx.learning.catalog.domain;

import java.util.Optional;

/** Port: where QuizLesson rows are stored. */
public interface QuizLessonRepository {
    Optional<QuizLesson> findByLessonId(String lessonId);

    void delete(QuizLesson entity);

    QuizLesson save(QuizLesson entity);
}
