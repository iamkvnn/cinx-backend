package com.cinx.learning.review.application;

import com.cinx.learning.review.application.command.QuizQuestionResult;
import com.cinx.learning.review.application.query.ReviewAnswerView;
import com.cinx.learning.review.application.query.ReviewTodayView;

import java.time.LocalDate;
import java.util.List;

/** Spaced review of official quiz questions (P3-03/P3-04): a Leitner queue that never touches official scores. */
public interface IReviewService {
    int DAILY_CAP = 20;
    /** The most active time one answer adds to the day: a question left open does not count as hours of study. */
    int MAX_SECONDS_PER_ANSWER = 300;

    /**
     * Called by the quiz submit, in its transaction: questions scored below full marks enter (or re-enter) the first
     * box. The caller passes only questions of a quiz whose review shows answers, and no essays.
     */
    void recordQuizResults(String userId, String courseId, String lessonId, List<QuizQuestionResult> results);

    ReviewTodayView today(String userId);

    ReviewAnswerView answer(String userId, String reviewItemId, String userAnswer, Integer elapsedSeconds, String idempotencyKey);

    long dueCount(String userId, LocalDate day);

    List<String> userIdsWithDue(LocalDate day);
}
