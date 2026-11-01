package com.cinx.learning.note.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
interface LessonNoteJpaRepository extends JpaRepository<LessonNoteJpaEntity, String> {
    List<LessonNoteJpaEntity> findByUserIdAndCourseIdAndLessonIdOrderByCreatedAtDesc(String userId, String courseId, String lessonId);

    List<LessonNoteJpaEntity> findByUserIdAndCourseIdOrderByCreatedAtDesc(String userId, String courseId);
}
