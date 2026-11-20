package com.cinx.learning.assessment.application;

import com.cinx.learning.assessment.application.query.InVideoAssessmentSubmissionView;

import java.util.List;

/** The answers learners give to the questions that stop a video. */
public interface InVideoAssessments {
    /** Stores the learner's answer, replacing an earlier answer to the same question. */
    void recordAnswer(String userId, String videoLessonId, String videoAssessmentId, String userAnswer);

    /** How many of the video's questions the learner has answered. */
    long answeredCount(String userId, String videoLessonId);

    List<InVideoAssessmentSubmissionView> submissions(String userId, String videoLessonId);
}
