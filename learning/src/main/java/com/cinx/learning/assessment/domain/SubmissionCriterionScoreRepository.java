package com.cinx.learning.assessment.domain;

import java.util.Collection;
import java.util.List;

/** Port for the stored SubmissionCriterionScore rows. */
public interface SubmissionCriterionScoreRepository {
    List<SubmissionCriterionScore> findAllBySubmissionIdIn(Collection<String> submissionIds);

    void deleteAllBySubmissionId(String submissionId);

    void deleteAllByCriterionIdIn(Collection<String> criterionIds);

    List<SubmissionCriterionScore> saveAll(Iterable<SubmissionCriterionScore> entities);
}
