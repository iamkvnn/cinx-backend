package com.cinx.learning.assessment.domain;

import java.util.List;
import java.util.Optional;

/** Port: where InVideoAssessmentSubmission rows are stored. */
public interface InVideoAssessmentSubmissionRepository {
    long countByUserIdAndVideoLessonId(String userId, String videoLessonId);

    Optional<InVideoAssessmentSubmission> findForUpdateByUserIdAndVideoAssessmentId(String userId, String videoAssessmentId);

    List<InVideoAssessmentSubmission> findByUserIdAndVideoLessonId(String userId, String videoLessonId);

    InVideoAssessmentSubmission save(InVideoAssessmentSubmission entity);
}
