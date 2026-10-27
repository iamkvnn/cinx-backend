package com.cinx.learning.courseview.application.query;

import com.cinx.learning.shared.domain.ScoringMode;

import java.util.List;

public record QuizLessonView (
        Integer numberOfQuestionPerQuizSession,
        Integer maxAttempt,
        Integer duration,
        Boolean isReviewAllowed,
        Boolean isShowAnswersOnReview,
        Boolean shuffleQuestions,
        Boolean shuffleOptions,
        ScoringMode scoringMode,
        List<QuizQuestionView> questions
){
}
