package com.cinx.learning.community.domain;

import java.util.Collection;
import java.util.List;

/** Port: where AnswerHelpfulMark rows are stored. */
public interface AnswerHelpfulMarkRepository {
    boolean existsByAnswerIdAndUserId(String answerId, String userId);

    long countByAnswerId(String answerId);

    void deleteByAnswerIdAndUserId(String answerId, String userId);

    void deleteByAnswerIdIn(Collection<String> answerIds);

    List<String> findAnswerIdsByUserIdAndAnswerIdIn(String userId, Collection<String> answerIds);

    AnswerHelpfulMark save(AnswerHelpfulMark entity);
}
