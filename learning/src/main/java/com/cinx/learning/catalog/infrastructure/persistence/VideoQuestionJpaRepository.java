package com.cinx.learning.catalog.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
interface VideoQuestionJpaRepository extends JpaRepository<VideoQuestionJpaEntity, String> {
    List<VideoQuestionJpaEntity> findByVideoLessonLessonIdOrderByTimestampSecondsAsc(String lessonId);
    Optional<VideoQuestionJpaEntity> findByIdAndVideoLessonLessonId(String id, String lessonId);
}

