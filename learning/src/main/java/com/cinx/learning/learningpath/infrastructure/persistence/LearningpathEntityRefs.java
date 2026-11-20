package com.cinx.learning.learningpath.infrastructure.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;

/** Points a stored row at another by its key, without reading the other row. */
@Component
public class LearningpathEntityRefs {
    @PersistenceContext
    private EntityManager entityManager;

    UserLearningPathJpaEntity userLearningPath(String id) {
        return id == null ? null : entityManager.getReference(UserLearningPathJpaEntity.class, id);
    }
}
