package com.cinx.learning.catalog.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizLesson {
    private String lessonId;
    private Integer duration;
    private Integer numberOfQuestionPerQuizSession;
    private Integer maxAttempt;
    @Builder.Default
    private Boolean isReviewAllowed = false;
    @Builder.Default
    private Boolean isShowAnswersOnReview = false;
    @Builder.Default
    private Boolean shuffleQuestions = false;
    @Builder.Default
    private Boolean shuffleOptions = false;
    @Builder.Default
    private ScoringMode scoringMode = ScoringMode.HIGHEST;
    @Builder.Default
    private Boolean hasPendingSync = false;
}
