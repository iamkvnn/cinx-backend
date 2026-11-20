package com.cinx.learning.review.infrastructure.persistence;

import com.cinx.learning.review.domain.ReviewItemStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

interface ReviewItemJpaRepository extends JpaRepository<ReviewItemJpaEntity, String> {
    Optional<ReviewItemJpaEntity> findByIdAndUserId(String id, String userId);

    List<ReviewItemJpaEntity> findByUserIdAndQuestionIdIn(String userId, Collection<String> questionIds);

    @Query("""
        SELECT r FROM ReviewItem r
        WHERE r.userId = :userId AND r.status = :status AND r.dueOn <= :day
        ORDER BY r.dueOn ASC, r.box ASC, r.createdAt ASC
    """)
    List<ReviewItemJpaEntity> findDue(@Param("userId") String userId, @Param("status") ReviewItemStatus status,
                                      @Param("day") LocalDate day, Pageable page);

    long countByUserIdAndStatusAndDueOnLessThanEqual(String userId, ReviewItemStatus status, LocalDate day);

    @Query("SELECT DISTINCT r.userId FROM ReviewItem r WHERE r.status = :status AND r.dueOn <= :day")
    List<String> findUserIdsWithDue(@Param("status") ReviewItemStatus status, @Param("day") LocalDate day);
}

interface ReviewAttemptJpaRepository extends JpaRepository<ReviewAttemptJpaEntity, String> {
    Optional<ReviewAttemptJpaEntity> findByUserIdAndIdempotencyKey(String userId, String idempotencyKey);
}
