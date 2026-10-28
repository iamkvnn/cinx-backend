package com.cinx.learning.catalog.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface AssignmentLessonJpaRepository extends JpaRepository<AssignmentLessonJpaEntity, String> {
    Optional<AssignmentLessonJpaEntity> findByLessonId(String lessonId);

    java.util.List<AssignmentLessonJpaEntity> findAllByLessonIdIn(java.util.Collection<String> lessonIds);
}

