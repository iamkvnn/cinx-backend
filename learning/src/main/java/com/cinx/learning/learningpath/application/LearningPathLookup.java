package com.cinx.learning.learningpath.application;

import com.cinx.learning.learningpath.application.query.UserLearningPathInfo;

import java.util.Optional;

/** What the other features read of a learner's learning path. */
public interface LearningPathLookup {
    /** The path the learner follows now: an active one, or one that waits for payment. */
    Optional<UserLearningPathInfo> currentPath(String userId);

    Optional<UserLearningPathInfo> path(String pathId);
}
