package com.cinx.learning.assessment.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface AssignmentCriterionJpaRepository extends JpaRepository<AssignmentCriterionJpaEntity, String> {
    List<AssignmentCriterionJpaEntity> findAllByAssignmentIdOrderByOrderIndexAsc(String assignmentId);
}
