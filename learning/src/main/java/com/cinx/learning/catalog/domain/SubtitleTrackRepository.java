package com.cinx.learning.catalog.domain;

import java.util.List;
import java.util.Optional;

/** Port: where SubtitleTrack rows are stored. */
public interface SubtitleTrackRepository {
    List<SubtitleTrack> findByVideoLessonLessonIdOrderByIsDefaultDescLanguageCodeAsc(String lessonId);

    Optional<SubtitleTrack> findByIdAndVideoLessonLessonId(String id, String lessonId);

    Optional<SubtitleTrack> findByVideoLessonLessonIdAndLanguageCode(String lessonId, String languageCode);

    List<SubtitleTrack> findByVideoLessonLessonIdAndIsDefaultTrue(String lessonId);

    long countByVideoLessonLessonId(String lessonId);

    void delete(SubtitleTrack entity);

    void deleteAll(Iterable<SubtitleTrack> entities);

    void flush();

    SubtitleTrack save(SubtitleTrack entity);

    List<SubtitleTrack> saveAll(Iterable<SubtitleTrack> entities);
}
