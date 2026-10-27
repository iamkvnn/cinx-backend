package com.cinx.learning.assessment.application;

import com.cinx.learning.assessment.application.query.QuizSessionInfo;
import com.cinx.learning.assessment.application.query.QuizSessionSubmissionInfo;
import com.cinx.learning.shared.domain.QuizSessionStatus;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/** What the other features read of quiz sessions and assignment submissions. */
public interface AssessmentLookup {
    /** Of these quiz lessons, the ones the learner has a session in one of the statuses for. */
    List<String> quizLessonIdsWithStatus(String userId, Collection<String> quizLessonIds, Collection<QuizSessionStatus> statuses);

    /** Of these assignments, the ones the learner has submitted. */
    List<String> submittedAssignmentIds(String userId, Collection<String> assignmentIds);

    List<QuizSessionInfo> quizSessionsOfCourse(String courseId, Collection<QuizSessionStatus> statuses);

    List<QuizSessionSubmissionInfo> submissionsOfQuizSessions(Collection<String> quizSessionIds);

    /** Per assignment, how many submissions wait for a grade and since when. */
    List<UngradedGroup> ungradedAssignmentGroups(Collection<String> assignmentIds);

    long gradedAssignmentSubmissionsSince(String assignmentId, LocalDateTime since);

    /** Per quiz lesson of these courses, how many sessions wait for an essay grade and since when. */
    List<PendingGradeGroup> pendingGradeGroups(Collection<String> courseIds);

    @Value
    class UngradedGroup {
        String assignmentId;
        long waiting;
        LocalDateTime oldest;
    }

    @Value
    class PendingGradeGroup {
        String courseId;
        String quizLessonId;
        long waiting;
        LocalDateTime oldest;
    }
}
