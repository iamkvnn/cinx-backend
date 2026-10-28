package com.cinx.learning.catalog.api.dto.response;

import com.cinx.learning.catalog.domain.ScoringMode;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record QuizLessonResponse(
        @Schema(example = "les_123")
        String lessonId,
        @Schema(example = "10")
        Integer numberOfQuestionPerQuizSession,
        @Schema(example = "3")
        Integer maxAttempt,
        @Schema(example = "1800")
        Integer duration,
        @Schema(example = "true")
        Boolean isReviewAllowed,
        @Schema(example = "true")
        Boolean isShowAnswersOnReview,
        @Schema(example = "true")
        Boolean shuffleQuestions,
        @Schema(example = "true")
        Boolean shuffleOptions,
        @Schema(example = "AVERAGE")
        ScoringMode scoringMode,
        @Schema(example = "false")
        Boolean hasPendingSync,
        List<QuizQuestionResponse> questions
){
    public QuizLessonResponse withoutReferenceAnswers() {
        return new QuizLessonResponse(lessonId, numberOfQuestionPerQuizSession, maxAttempt, duration, isReviewAllowed,
                isShowAnswersOnReview, shuffleQuestions, shuffleOptions, scoringMode, hasPendingSync,
                questions == null ? null : questions.stream().map(QuizQuestionResponse::withoutReferenceAnswer).toList());
    }

    public QuizLessonResponse withoutAnswerKeys() {
        return new QuizLessonResponse(lessonId, numberOfQuestionPerQuizSession, maxAttempt, duration, isReviewAllowed,
                isShowAnswersOnReview, shuffleQuestions, shuffleOptions, scoringMode, hasPendingSync,
                questions == null ? null : questions.stream().map(QuizQuestionResponse::withoutAnswerKeys).toList());
    }
}

