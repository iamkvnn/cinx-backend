package com.cinx.learning.review.domain;

import java.util.Optional;

/** Port: where review answers are stored. */
public interface ReviewAttemptRepository {
    Optional<ReviewAttempt> findByUserIdAndIdempotencyKey(String userId, String idempotencyKey);

    ReviewAttempt insert(ReviewAttempt attempt);
}
