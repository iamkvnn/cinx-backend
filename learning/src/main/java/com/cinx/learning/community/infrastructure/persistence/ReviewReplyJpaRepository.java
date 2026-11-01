package com.cinx.learning.community.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
interface ReviewReplyJpaRepository extends JpaRepository<ReviewReplyJpaEntity, String> {
    Optional<ReviewReplyJpaEntity> findByReviewId(String reviewId);
    List<ReviewReplyJpaEntity> findByReviewIdIn(List<String> reviewIds);
    void deleteByReviewId(String reviewId);

    @Query("""
        SELECT COUNT(rr)
        FROM ReviewReply rr, Review r
        WHERE rr.reviewId = r.id
            AND r.courseId = :courseId
    """)
    Long countRepliesByCourseId(String courseId);
}
