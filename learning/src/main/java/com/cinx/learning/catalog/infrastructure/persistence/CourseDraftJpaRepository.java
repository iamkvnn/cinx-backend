package com.cinx.learning.catalog.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

interface CourseDraftJpaRepository extends JpaRepository<CourseDraftJpaEntity, String> {
    @Query("""
        SELECT d
        FROM CourseDraft d
        JOIN d.course c
        WHERE c.id = :courseId
    """)
    Optional<CourseDraftJpaEntity> findByCourse(@Param("courseId") String courseId);
}

