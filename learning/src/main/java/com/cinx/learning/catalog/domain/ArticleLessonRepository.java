package com.cinx.learning.catalog.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Port: where ArticleLesson rows are stored. */
public interface ArticleLessonRepository {
    Optional<ArticleLesson> findByLessonId(String lessonId);

    java.util.List<ArticleLesson> findAllByLessonIdIn(java.util.Collection<String> lessonIds);

    void delete(ArticleLesson entity);

    ArticleLesson save(ArticleLesson entity);
}
