package com.cinx.learning.catalog.domain;

import java.util.List;
import java.util.Optional;

/** Port: where VideoQuestion rows are stored. */
public interface VideoQuestionRepository {
    List<VideoQuestion> findByVideoLessonLessonIdOrderByTimestampSecondsAsc(String lessonId);

    Optional<VideoQuestion> findByIdAndVideoLessonLessonId(String id, String lessonId);

    void delete(VideoQuestion entity);

    void deleteAll(Iterable<VideoQuestion> entities);

    Optional<VideoQuestion> findById(String id);

    VideoQuestion save(VideoQuestion entity);
}
