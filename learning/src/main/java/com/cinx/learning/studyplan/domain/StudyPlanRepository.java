package com.cinx.learning.studyplan.domain;

import java.util.List;
import java.util.Optional;

/** Port for the stored StudyPlan rows. */
public interface StudyPlanRepository {
    Optional<StudyPlan> findByUserId(String userId);

    /** Every learner who has a plan. */
    List<String> findAllUserIds();

    StudyPlan save(StudyPlan entity);
}
