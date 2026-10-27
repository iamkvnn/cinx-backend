package com.cinx.learning.community.domain;

import java.util.List;
import java.util.Optional;

/** Port: where ReviewReply rows are stored. */
public interface ReviewReplyRepository {
    Optional<ReviewReply> findByReviewId(String reviewId);

    List<ReviewReply> findByReviewIdIn(List<String> reviewIds);

    void deleteByReviewId(String reviewId);

    Long countRepliesByCourseId(String courseId);

    void delete(ReviewReply entity);

    Optional<ReviewReply> findById(String id);

    ReviewReply save(ReviewReply entity);
}
