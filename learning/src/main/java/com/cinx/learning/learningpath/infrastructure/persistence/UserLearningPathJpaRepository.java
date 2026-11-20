package com.cinx.learning.learningpath.infrastructure.persistence;

import com.cinx.learning.shared.domain.LearningPathStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
interface UserLearningPathJpaRepository extends JpaRepository<UserLearningPathJpaEntity, String> {
    List<UserLearningPathJpaEntity> findByUserId(String userId);
    Optional<UserLearningPathJpaEntity> findByUserIdAndStatus(String userId, LearningPathStatus status);
    Optional<UserLearningPathJpaEntity> findByUserIdAndStatusIn(String userId, List<LearningPathStatus> statuses);
    Optional<UserLearningPathJpaEntity> findByUserIdAndIdempotencyKey(String userId, String idempotencyKey);
}
