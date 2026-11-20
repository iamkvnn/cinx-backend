package com.cinx.learning.assessment.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface AssignmentSubmissionAttachmentJpaRepository extends JpaRepository<AssignmentSubmissionAttachmentJpaEntity, String> {
}
