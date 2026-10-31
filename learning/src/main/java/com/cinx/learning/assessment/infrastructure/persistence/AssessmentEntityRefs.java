package com.cinx.learning.assessment.infrastructure.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;

/** Points a stored row at another by its key, without reading the other row. */
@Component
public class AssessmentEntityRefs {
    @PersistenceContext
    private EntityManager entityManager;

    AssignmentSubmissionJpaEntity assignmentSubmission(String id) {
        return id == null ? null : entityManager.getReference(AssignmentSubmissionJpaEntity.class, id);
    }
}
