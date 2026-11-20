package com.cinx.learning.assessment.application;

import com.cinx.learning.assessment.application.query.PracticeCheckView;
import com.cinx.learning.assessment.application.query.PracticeQuestionView;

import java.util.Map;
import java.util.Optional;

/**
 * Official quiz questions practised outside a quiz session (spaced review). Reads go through the signed-in learner's
 * lesson access; nothing here touches a quiz session, a score or progress.
 */
public interface QuizPractice {
    /** Whether a quiz's questions may be reviewed at all: the learner may see their attempts and the answers. */
    static boolean reviewable(Boolean reviewAllowed, Boolean showAnswersOnReview) {
        return Boolean.TRUE.equals(reviewAllowed) && Boolean.TRUE.equals(showAnswersOnReview);
    }

    /**
     * The lesson's reviewable questions by id. Empty when the learner cannot read the lesson now (their review waits);
     * an empty map when they can but nothing in it is reviewable any more (the quiz hides answers, the question is gone).
     */
    Optional<Map<String, PracticeQuestionView>> questions(String courseId, String lessonId);

    /** Grades an answer exactly as a quiz session would; empty when the question is no longer reviewable. */
    Optional<PracticeCheckView> check(String courseId, String lessonId, String questionId, String userAnswer);
}
