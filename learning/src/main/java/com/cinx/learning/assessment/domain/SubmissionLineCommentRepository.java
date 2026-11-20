package com.cinx.learning.assessment.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Port for the stored SubmissionLineComment rows. */
public interface SubmissionLineCommentRepository {
    List<SubmissionLineComment> findAllBySubmissionIdInOrderByLineAsc(Collection<String> submissionIds);

    void delete(SubmissionLineComment entity);

    Optional<SubmissionLineComment> findById(String id);

    SubmissionLineComment save(SubmissionLineComment entity);
}
