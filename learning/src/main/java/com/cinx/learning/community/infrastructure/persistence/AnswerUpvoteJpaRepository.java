package com.cinx.learning.community.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;

@Repository
interface AnswerUpvoteJpaRepository extends JpaRepository<AnswerUpvoteJpaEntity, String> {
    Optional<AnswerUpvoteJpaEntity> findByAnswerIdAndUserId(String answerId, String userId);
    boolean existsByAnswerIdAndUserId(String answerId, String userId);
    void deleteByAnswerIdIn(Collection<String> answerIds);
}
