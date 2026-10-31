package com.cinx.learning.assessment.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

interface AssignmentSubmissionJpaRepository extends JpaRepository<AssignmentSubmissionJpaEntity, String> {
    Page<AssignmentSubmissionJpaEntity> findAllByAssignmentId(String assignmentId, Pageable pageable);

    List<AssignmentSubmissionJpaEntity> findAllByAssignmentId(String assignmentId);

    /** Ungraded submissions of these assignments, grouped per assignment: count and oldest submission time. */
    @Query("""
        SELECT s.assignmentId AS assignmentId, COUNT(s) AS waiting, MIN(s.submissionTime) AS oldest
        FROM AssignmentSubmission s
        WHERE s.assignmentId IN :assignmentIds AND s.score IS NULL
        GROUP BY s.assignmentId
    """)
    List<UngradedGroup> findUngradedGroups(@Param("assignmentIds") Collection<String> assignmentIds);

    @Query("SELECT COUNT(s) FROM AssignmentSubmission s WHERE s.assignmentId = :assignmentId AND s.gradedAt >= :since")
    long countGradedSince(@Param("assignmentId") String assignmentId, @Param("since") java.time.LocalDateTime since);

    /** Scores of a learner's submissions for these assignments. */
    List<AssignmentSubmissionJpaEntity> findAllByUserIdAndAssignmentIdIn(String userId, Collection<String> assignmentIds);

    interface UngradedGroup {
        String getAssignmentId();
        long getWaiting();
        java.time.LocalDateTime getOldest();
    }

    Optional<AssignmentSubmissionJpaEntity> findByUserIdAndAssignmentId(String userId, String assignmentId);

    /** Which of these assignment lessons the learner has submitted (prerequisite rule: submitted counts). */
    @Query("SELECT DISTINCT s.assignmentId FROM AssignmentSubmission s WHERE s.userId = :userId AND s.assignmentId IN :assignmentIds")
    List<String> findSubmittedAssignmentIds(@Param("userId") String userId, @Param("assignmentIds") Collection<String> assignmentIds);
}
