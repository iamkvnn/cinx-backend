package com.cinx.learning.assessment.application;

import com.cinx.common.paging.PageResult;

import com.cinx.learning.assessment.application.command.CreateAssignmentSubmissionCommand;
import com.cinx.learning.assessment.application.query.AssignmentSubmissionView;

public interface IAssignmentService {
    PageResult<AssignmentSubmissionView> getAssignmentSubmissions(String assignmentId, int page, int size, String sort);
    AssignmentSubmissionView getAssignmentSubmission(String userId, String assignmentId);
    void submitAssignment(String userId, String assignmentId, CreateAssignmentSubmissionCommand request);
    void scoreAssignmentSubmission(String currentUserId, String submissionId, Double score);
    void deleteAssignmentSubmission(String currentUserId, String submissionId);
}
