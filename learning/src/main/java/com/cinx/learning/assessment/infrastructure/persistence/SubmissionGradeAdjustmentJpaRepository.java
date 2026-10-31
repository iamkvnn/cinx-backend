package com.cinx.learning.assessment.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

interface SubmissionGradeAdjustmentJpaRepository extends JpaRepository<SubmissionGradeAdjustmentJpaEntity, String> {
    List<SubmissionGradeAdjustmentJpaEntity> findAllBySubmissionIdIn(Collection<String> submissionIds);

    @Modifying
    @Query("DELETE FROM SubmissionGradeAdjustment a WHERE a.submissionId = :submissionId")
    void deleteAllBySubmissionId(@Param("submissionId") String submissionId);
}
