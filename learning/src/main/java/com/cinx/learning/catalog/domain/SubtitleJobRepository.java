package com.cinx.learning.catalog.domain;

import java.util.List;
import java.util.Optional;

/** Port: where SubtitleJob rows are stored. */
public interface SubtitleJobRepository {
    List<SubtitleJob> findByVideoLessonLessonIdOrderByCreatedAtDesc(String lessonId);

    Optional<SubtitleJob> findByIdAndVideoLessonLessonId(String id, String lessonId);

    boolean existsByVideoLessonLessonIdAndTargetLanguageCodeAndStatusIn(String lessonId, String targetLanguageCode, List<SubtitleJobStatus> statuses);

    void deleteAll(Iterable<SubtitleJob> entities);

    Optional<SubtitleJob> findById(String id);

    SubtitleJob save(SubtitleJob entity);
}
