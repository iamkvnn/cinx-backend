package com.cinx.learning.assessment.domain;

import java.util.List;

/** Port for the stored AssignmentCriterion rows. */
public interface AssignmentCriterionRepository {
    List<AssignmentCriterion> findAllByAssignmentIdOrderByOrderIndexAsc(String assignmentId);

    void deleteAll(Iterable<AssignmentCriterion> entities);

    List<AssignmentCriterion> saveAll(Iterable<AssignmentCriterion> entities);
}
