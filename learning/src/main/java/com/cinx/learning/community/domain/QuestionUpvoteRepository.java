package com.cinx.learning.community.domain;

import java.util.Optional;

/** Port: where QuestionUpvote rows are stored. */
public interface QuestionUpvoteRepository {
    Optional<QuestionUpvote> findByQuestionIdAndUserId(String questionId, String userId);

    boolean existsByQuestionIdAndUserId(String questionId, String userId);

    void deleteByQuestionId(String questionId);

    void delete(QuestionUpvote entity);

    QuestionUpvote save(QuestionUpvote entity);
}
