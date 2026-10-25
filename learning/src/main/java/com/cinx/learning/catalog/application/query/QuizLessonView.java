package com.cinx.learning.catalog.application.query;

import com.cinx.learning.catalog.domain.ScoringMode;

import java.util.List;

public record QuizLessonView(
        String lessonId,
        Integer numberOfQuestionPerQuizSession,
        Integer maxAttempt,
        Integer duration,
        Boolean isReviewAllowed,
        Boolean isShowAnswersOnReview,
        Boolean shuffleQuestions,
        Boolean shuffleOptions,
        ScoringMode scoringMode,
        Boolean hasPendingSync,
        List<QuizQuestionView> questions
){
    public QuizLessonView withoutReferenceAnswers() {
        return new QuizLessonView(lessonId, numberOfQuestionPerQuizSession, maxAttempt, duration, isReviewAllowed,
                isShowAnswersOnReview, shuffleQuestions, shuffleOptions, scoringMode, hasPendingSync,
                questions == null ? null : questions.stream().map(QuizQuestionView::withoutReferenceAnswer).toList());
    }

    public QuizLessonView withoutAnswerKeys() {
        return new QuizLessonView(lessonId, numberOfQuestionPerQuizSession, maxAttempt, duration, isReviewAllowed,
                isShowAnswersOnReview, shuffleQuestions, shuffleOptions, scoringMode, hasPendingSync,
                questions == null ? null : questions.stream().map(QuizQuestionView::withoutAnswerKeys).toList());
    }
}

