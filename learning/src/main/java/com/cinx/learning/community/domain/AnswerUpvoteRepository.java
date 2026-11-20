package com.cinx.learning.community.domain;

import java.util.Collection;
import java.util.Optional;

/** Port: where AnswerUpvote rows are stored. */
public interface AnswerUpvoteRepository {
    Optional<AnswerUpvote> findByAnswerIdAndUserId(String answerId, String userId);

    boolean existsByAnswerIdAndUserId(String answerId, String userId);

    void deleteByAnswerIdIn(Collection<String> answerIds);

    void delete(AnswerUpvote entity);

    AnswerUpvote save(AnswerUpvote entity);
}
