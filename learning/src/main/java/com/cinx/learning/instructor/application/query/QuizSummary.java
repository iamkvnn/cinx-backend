package com.cinx.learning.instructor.application.query;

/**
 * One quiz of a course. averageScore is the mean of each learner's latest submitted score (0–10); passRate is the
 * share of those learners whose item progress is passed (0–100). Both are null before the first attempt.
 */
public record QuizSummary(
        String lessonId,
        String title,
        int position,
        long questionCount,
        long attempts,
        long learnersAttempted,
        Double averageScore,
        Double passRate,
        long pendingEssays
) {
}
