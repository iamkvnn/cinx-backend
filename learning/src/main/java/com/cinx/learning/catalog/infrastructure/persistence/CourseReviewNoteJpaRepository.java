package com.cinx.learning.catalog.infrastructure.persistence;

import com.cinx.learning.shared.domain.ReviewNoteStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface CourseReviewNoteJpaRepository extends JpaRepository<CourseReviewNoteJpaEntity, String> {
    List<CourseReviewNoteJpaEntity> findAllByCourseIdAndStatusOrderByCreatedAtAsc(String courseId, ReviewNoteStatus status);

    List<CourseReviewNoteJpaEntity> findAllByCourseId(String courseId);
}
