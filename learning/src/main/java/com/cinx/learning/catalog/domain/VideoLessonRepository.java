package com.cinx.learning.catalog.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Port: where VideoLesson rows are stored. */
public interface VideoLessonRepository {
    Optional<VideoLesson> findByLessonId(String lessonId);

    List<VideoLesson> findAllByLessonIdIn(Collection<String> lessonIds);

    void delete(VideoLesson entity);

    VideoLesson save(VideoLesson entity);
}
