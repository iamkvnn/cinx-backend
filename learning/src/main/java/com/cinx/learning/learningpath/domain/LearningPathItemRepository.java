package com.cinx.learning.learningpath.domain;

import java.util.List;
import java.util.Optional;

/** Port: where LearningPathItem rows are stored. */
public interface LearningPathItemRepository {
    Optional<LearningPathItem> findByLearningPathIdAndLessonId(String learningPathId, String lessonId);

    LearningPathItem save(LearningPathItem entity);

    List<LearningPathItem> saveAll(Iterable<LearningPathItem> entities);
}
