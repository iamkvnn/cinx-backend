package com.cinx.learning.courseview.api.dto.response;

import com.cinx.learning.shared.domain.ScoringMode;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record QuizLessonResponse (
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
        @Schema(example = "false")
        Boolean shuffleQuestions,
        @Schema(example = "false")
        Boolean shuffleOptions,
        @Schema(example = "HIGHEST_SCORE")
        ScoringMode scoringMode,
        List<QuizQuestionResponse> questions
){
}
