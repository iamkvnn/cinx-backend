package com.cinx.learning.learningpath.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
interface LearningPathItemJpaRepository extends JpaRepository<LearningPathItemJpaEntity, String> {
    List<LearningPathItemJpaEntity> findByLearningPathIdOrderByOrderIndexAsc(String learningPathId);
    Optional<LearningPathItemJpaEntity> findFirstByLearningPathIdAndIsCompletedFalseOrderByOrderIndexAsc(String learningPathId);
    Optional<LearningPathItemJpaEntity> findByLearningPathIdAndLessonId(String learningPathId, String lessonId);
}
