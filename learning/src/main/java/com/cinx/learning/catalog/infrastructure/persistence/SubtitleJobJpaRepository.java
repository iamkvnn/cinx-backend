package com.cinx.learning.catalog.infrastructure.persistence;

import com.cinx.learning.catalog.domain.SubtitleJobStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface SubtitleJobJpaRepository extends JpaRepository<SubtitleJobJpaEntity, String> {
    List<SubtitleJobJpaEntity> findByVideoLessonLessonIdOrderByCreatedAtDesc(String lessonId);

    Optional<SubtitleJobJpaEntity> findByIdAndVideoLessonLessonId(String id, String lessonId);

    boolean existsByVideoLessonLessonIdAndTargetLanguageCodeAndStatusIn(
            String lessonId,
            String targetLanguageCode,
            List<SubtitleJobStatus> statuses
    );
}

