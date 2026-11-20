package com.cinx.learning.catalog.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface QuizLessonJpaRepository extends JpaRepository<QuizLessonJpaEntity, String> {
    Optional<QuizLessonJpaEntity> findByLessonId(String lessonId);
}

