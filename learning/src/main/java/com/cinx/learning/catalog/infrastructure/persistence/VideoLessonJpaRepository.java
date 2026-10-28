package com.cinx.learning.catalog.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

interface VideoLessonJpaRepository extends JpaRepository<VideoLessonJpaEntity, String> {
    Optional<VideoLessonJpaEntity> findByLessonId(String lessonId);

    List<VideoLessonJpaEntity> findAllByLessonIdIn(Collection<String> lessonIds);
}

