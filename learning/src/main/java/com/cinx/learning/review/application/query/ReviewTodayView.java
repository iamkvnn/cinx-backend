package com.cinx.learning.review.application.query;

import com.cinx.learning.assessment.application.query.QuizSessionOptionView;
import com.cinx.learning.shared.domain.QuizQuestionType;
import com.cinx.learning.shared.domain.ScoringMethod;

import java.util.List;

/**
 * Today's review queue: at most {@code dailyCap} questions, the most overdue first.
 *
 * @param dueTotal every question due by today, including those past the cap
 */
public record ReviewTodayView(long dueTotal, int dailyCap, List<Item> items) {

    /** A question to answer: never its key, which only the answer's result carries. */
    public record Item(
            String reviewItemId,
            String courseId,
            String courseTitle,
            String lessonId,
            String lessonTitle,
            int box,
            String questionId,
            String questionText,
            QuizQuestionType questionType,
            ScoringMethod scoringMethod,
            List<QuizSessionOptionView> options) {
    }
}
