package com.cinx.learning.catalog.api.dto.request;

import com.cinx.learning.catalog.domain.ScoringMode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CreateQuizLessonRequest {
    @NotNull
    @Min(1)
    @Schema(example = "10")
    private Integer numberOfQuestionPerQuizSession;
    @Min(1)
    @Schema(example = "3")
    private Integer maxAttempt;
    @Min(0)
    @Schema(example = "1800")
    private Integer duration;
    @NotNull
    @Schema(example = "true")
    private Boolean isReviewAllowed;
    @NotNull
    @Schema(example = "true")
    private Boolean isShowAnswersOnReview;
    @NotNull
    @Schema(example = "true")
    private Boolean shuffleQuestions;
    @NotNull
    @Schema(example = "true")
    private Boolean shuffleOptions;
    @NotNull
    private ScoringMode scoringMode;
    @NotEmpty
    @Valid
    private List<CreateQuizQuestionRequest> questions;
}

