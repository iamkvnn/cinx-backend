package com.cinx.learning.catalog.infrastructure.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;

/** Points a stored row at another by its key, without reading the other row. */
@Component
public class CatalogEntityRefs {
    @PersistenceContext
    private EntityManager entityManager;

    AssignmentLessonJpaEntity assignmentLesson(String id) {
        return id == null ? null : entityManager.getReference(AssignmentLessonJpaEntity.class, id);
    }

    CategoryJpaEntity category(String id) {
        return id == null ? null : entityManager.getReference(CategoryJpaEntity.class, id);
    }

    CourseJpaEntity course(String id) {
        return id == null ? null : entityManager.getReference(CourseJpaEntity.class, id);
    }

    CourseDraftJpaEntity courseDraft(String id) {
        return id == null ? null : entityManager.getReference(CourseDraftJpaEntity.class, id);
    }

    QuizLessonJpaEntity quizLesson(String id) {
        return id == null ? null : entityManager.getReference(QuizLessonJpaEntity.class, id);
    }

    QuizQuestionJpaEntity quizQuestion(String id) {
        return id == null ? null : entityManager.getReference(QuizQuestionJpaEntity.class, id);
    }

    SectionJpaEntity section(String id) {
        return id == null ? null : entityManager.getReference(SectionJpaEntity.class, id);
    }

    VideoLessonJpaEntity videoLesson(String id) {
        return id == null ? null : entityManager.getReference(VideoLessonJpaEntity.class, id);
    }

    VideoQuestionJpaEntity videoQuestion(String id) {
        return id == null ? null : entityManager.getReference(VideoQuestionJpaEntity.class, id);
    }
}
