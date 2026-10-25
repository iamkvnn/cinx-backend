package com.cinx.learning.catalog.application.command;

import com.cinx.learning.catalog.domain.ScoringMode;
import lombok.Data;

@Data
public class UpdateQuizLessonCommand {
    private Integer numberOfQuestionPerQuizSession;
    private Integer maxAttempt;
    private Integer duration;
    private Boolean isReviewAllowed;
    private Boolean isShowAnswersOnReview;
    private Boolean shuffleQuestions;
    private Boolean shuffleOptions;
    private ScoringMode scoringMode;
}

