package com.cinx.learning.assessment.domain;

import com.cinx.common.paging.PageQuery;
import com.cinx.common.paging.PageResult;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Port: where AssignmentSubmission rows are stored. */
public interface AssignmentSubmissionRepository {
    PageResult<AssignmentSubmission> findAllByAssignmentId(String assignmentId, PageQuery pageable);

    List<AssignmentSubmission> findAllByAssignmentId(String assignmentId);

    /** Ungraded submissions of these assignments, grouped per assignment: count and oldest submission time. */
    List<UngradedGroup> findUngradedGroups(Collection<String> assignmentIds);

    long countGradedSince(String assignmentId, java.time.LocalDateTime since);

    Optional<AssignmentSubmission> findByUserIdAndAssignmentId(String userId, String assignmentId);

    /** Which of these assignment lessons the learner has submitted (prerequisite rule: submitted counts). */
    List<String> findSubmittedAssignmentIds(String userId, Collection<String> assignmentIds);

    void deleteById(String id);

    Optional<AssignmentSubmission> findById(String id);

    AssignmentSubmission save(AssignmentSubmission entity);

    AssignmentSubmission saveAndFlush(AssignmentSubmission entity);

    interface UngradedGroup {
            String getAssignmentId();
            long getWaiting();
            java.time.LocalDateTime getOldest();
        }
}
