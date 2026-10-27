package com.cinx.learning.assessment.application;

import com.cinx.learning.assessment.application.command.GradeSubmissionCommand;
import com.cinx.learning.assessment.application.command.LineCommentCommand;
import com.cinx.learning.assessment.application.command.SaveCriteriaCommand;
import com.cinx.learning.assessment.application.query.AssignmentGradingView;

import java.util.List;

/** Grading from the instructor inbox: criteria, per-criterion scores, adjustments, feedback and line comments. */
public interface IAssignmentGradingService {
    AssignmentGradingView getGrading(String currentUserId, String lessonId);

    List<AssignmentGradingView.Criterion> saveCriteria(String currentUserId, String lessonId, SaveCriteriaCommand request);

    AssignmentGradingView.Submission grade(String currentUserId, String submissionId, GradeSubmissionCommand request);

    AssignmentGradingView.LineComment addComment(String currentUserId, String submissionId, LineCommentCommand request);

    void deleteComment(String currentUserId, String submissionId, String commentId);

    AssignmentGradingView.AttachmentText readAttachmentText(String currentUserId, String submissionId, String attachmentId);
}
