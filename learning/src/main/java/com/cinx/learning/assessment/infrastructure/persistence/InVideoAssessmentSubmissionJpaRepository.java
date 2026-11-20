package com.cinx.learning.assessment.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

interface InVideoAssessmentSubmissionJpaRepository extends JpaRepository<InVideoAssessmentSubmissionJpaEntity, String> {
    long countByUserIdAndVideoLessonId(String userId, String videoLessonId);
    boolean existsByUserIdAndVideoAssessmentId(String userId, String videoAssessmentId);
    Optional<InVideoAssessmentSubmissionJpaEntity> findByUserIdAndVideoAssessmentId(String userId, String videoAssessmentId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM InVideoAssessmentSubmission s WHERE s.userId = :userId AND s.videoAssessmentId = :videoAssessmentId")
    Optional<InVideoAssessmentSubmissionJpaEntity> findForUpdateByUserIdAndVideoAssessmentId(String userId, String videoAssessmentId);
    List<InVideoAssessmentSubmissionJpaEntity> findByUserIdAndVideoLessonId(String userId, String videoLessonId);
}
