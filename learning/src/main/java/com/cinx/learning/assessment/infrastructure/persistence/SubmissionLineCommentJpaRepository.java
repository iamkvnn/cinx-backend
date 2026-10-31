package com.cinx.learning.assessment.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

interface SubmissionLineCommentJpaRepository extends JpaRepository<SubmissionLineCommentJpaEntity, String> {
    List<SubmissionLineCommentJpaEntity> findAllBySubmissionIdInOrderByLineAsc(Collection<String> submissionIds);
}
