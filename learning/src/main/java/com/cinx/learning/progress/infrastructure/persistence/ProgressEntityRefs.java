package com.cinx.learning.progress.infrastructure.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;

/** Points a stored row at another by its key, without reading the other row. */
@Component
public class ProgressEntityRefs {
    @PersistenceContext
    private EntityManager entityManager;

    CourseProgressJpaEntity courseProgress(String id) {
        return id == null ? null : entityManager.getReference(CourseProgressJpaEntity.class, id);
    }
}
