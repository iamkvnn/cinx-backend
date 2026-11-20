package com.cinx.learning.catalog.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface SubtitleTrackJpaRepository extends JpaRepository<SubtitleTrackJpaEntity, String> {
    List<SubtitleTrackJpaEntity> findByVideoLessonLessonIdOrderByIsDefaultDescLanguageCodeAsc(String lessonId);

    Optional<SubtitleTrackJpaEntity> findByIdAndVideoLessonLessonId(String id, String lessonId);

    Optional<SubtitleTrackJpaEntity> findByVideoLessonLessonIdAndLanguageCode(String lessonId, String languageCode);

    List<SubtitleTrackJpaEntity> findByVideoLessonLessonIdAndIsDefaultTrue(String lessonId);

    long countByVideoLessonLessonId(String lessonId);
}

