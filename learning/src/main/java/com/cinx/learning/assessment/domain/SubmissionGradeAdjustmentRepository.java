package com.cinx.learning.assessment.domain;

import java.util.Collection;
import java.util.List;

/** Port for the stored SubmissionGradeAdjustment rows. */
public interface SubmissionGradeAdjustmentRepository {
    List<SubmissionGradeAdjustment> findAllBySubmissionIdIn(Collection<String> submissionIds);

    void deleteAllBySubmissionId(String submissionId);

    List<SubmissionGradeAdjustment> saveAll(Iterable<SubmissionGradeAdjustment> entities);
}
