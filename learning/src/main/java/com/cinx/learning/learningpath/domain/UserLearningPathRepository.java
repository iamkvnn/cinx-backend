package com.cinx.learning.learningpath.domain;

import com.cinx.learning.shared.domain.LearningPathStatus;

import java.util.List;
import java.util.Optional;

/** Port: where UserLearningPath rows are stored. */
public interface UserLearningPathRepository {
    List<UserLearningPath> findByUserId(String userId);

    Optional<UserLearningPath> findByUserIdAndStatus(String userId, LearningPathStatus status);

    Optional<UserLearningPath> findByUserIdAndStatusIn(String userId, List<LearningPathStatus> statuses);

    Optional<UserLearningPath> findByUserIdAndIdempotencyKey(String userId, String idempotencyKey);

    Optional<UserLearningPath> findById(String id);

    UserLearningPath save(UserLearningPath entity);

    UserLearningPath saveAndFlush(UserLearningPath entity);
}
