package com.cinx.learning.community.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface ReviewReactionJpaRepository extends JpaRepository<ReviewReactionJpaEntity, String> {
    Optional<ReviewReactionJpaEntity> findByUserIdAndReviewId(String userId, String reviewId);
    List<ReviewReactionJpaEntity> findByReviewId(String reviewId);
    List<ReviewReactionJpaEntity> findByReviewIdIn(List<String> reviewIds);
    void deleteByReviewId(String reviewId);
}
