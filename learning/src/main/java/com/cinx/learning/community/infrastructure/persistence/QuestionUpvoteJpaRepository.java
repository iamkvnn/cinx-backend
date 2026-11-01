package com.cinx.learning.community.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
interface QuestionUpvoteJpaRepository extends JpaRepository<QuestionUpvoteJpaEntity, String> {
    Optional<QuestionUpvoteJpaEntity> findByQuestionIdAndUserId(String questionId, String userId);
    boolean existsByQuestionIdAndUserId(String questionId, String userId);
    void deleteByQuestionId(String questionId);
}
