package com.cinx.learning.assessment.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

interface SubmissionCriterionScoreJpaRepository extends JpaRepository<SubmissionCriterionScoreJpaEntity, String> {
    List<SubmissionCriterionScoreJpaEntity> findAllBySubmissionIdIn(Collection<String> submissionIds);

    @Modifying
    @Query("DELETE FROM SubmissionCriterionScore s WHERE s.submissionId = :submissionId")
    void deleteAllBySubmissionId(@Param("submissionId") String submissionId);

    @Modifying
    @Query("DELETE FROM SubmissionCriterionScore s WHERE s.criterionId IN :criterionIds")
    void deleteAllByCriterionIdIn(@Param("criterionIds") Collection<String> criterionIds);
}
