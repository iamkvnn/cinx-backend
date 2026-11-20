package com.cinx.learning.catalog.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface ArticleLessonJpaRepository extends JpaRepository<ArticleLessonJpaEntity, String> {
    Optional<ArticleLessonJpaEntity> findByLessonId(String lessonId);

    java.util.List<ArticleLessonJpaEntity> findAllByLessonIdIn(java.util.Collection<String> lessonIds);
}

