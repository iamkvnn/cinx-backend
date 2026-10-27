package com.cinx.learning.assessment.domain;

import com.cinx.learning.shared.domain.QuizQuestionType;

import com.cinx.learning.shared.domain.ScoringMethod;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import com.cinx.learning.shared.domain.Model;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class QuizSessionQuestion extends Model {
    private String quizSessionId;
    private String questionId;
    private QuizQuestionType questionType;
    private Integer questionOrder;
    private String questionText;
    private String correctAnswer;
    private String userAnswer;
    private ScoringMethod scoringMethod;
    private Double score;
    private String optionsSnapshot;
    /** Copied from the question when the attempt starts; shown only on review when the quiz shows answers. */
    private String explanation;
    /** Copied from the question when the attempt starts, like the explanation; always shown. */
    @Builder.Default
    private Boolean aiAssisted = false;
}
