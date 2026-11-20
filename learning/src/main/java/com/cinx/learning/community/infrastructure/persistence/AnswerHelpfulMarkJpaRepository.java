package com.cinx.learning.community.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
interface AnswerHelpfulMarkJpaRepository extends JpaRepository<AnswerHelpfulMarkJpaEntity, String> {
    boolean existsByAnswerIdAndUserId(String answerId, String userId);

    long countByAnswerId(String answerId);

    void deleteByAnswerIdAndUserId(String answerId, String userId);

    void deleteByAnswerIdIn(Collection<String> answerIds);

    @Query("""
            select mark.answerId
            from AnswerHelpfulMark mark
            where mark.userId = :userId
              and mark.answerId in :answerIds
            """)
    List<String> findAnswerIdsByUserIdAndAnswerIdIn(String userId, Collection<String> answerIds);
}
