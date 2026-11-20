package com.cinx.learning.community.domain;

import java.util.List;
import java.util.Optional;

/** Port: where ReviewReaction rows are stored. */
public interface ReviewReactionRepository {
    Optional<ReviewReaction> findByUserIdAndReviewId(String userId, String reviewId);

    List<ReviewReaction> findByReviewId(String reviewId);

    List<ReviewReaction> findByReviewIdIn(List<String> reviewIds);

    void deleteByReviewId(String reviewId);

    ReviewReaction save(ReviewReaction entity);
}
