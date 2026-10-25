package com.cinx.learning.catalog.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import com.cinx.learning.shared.domain.Model;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class QuizQuestion extends Model {
    private String questionText;
    private QuizQuestionType questionType;
    @Builder.Default
    private ScoringMethod scoringMethod = ScoringMethod.ALL_OR_NOTHING;
    @Builder.Default
    private Boolean needSync = false;
    /** Essay questions only; instructor-facing, never part of what a learner receives. */
    private String referenceAnswer;
    /** Why the answer is right; a learner sees it only on review when the quiz shows answers. */
    private String explanation;
    /** An AI draft helped write the question; only ever turned on. */
    @Builder.Default
    private Boolean aiAssisted = false;
    private String quizLessonId;
    /** Read-only: filled when the question is loaded; the options are stored through their own repository. */
    @Builder.Default
    private List<QuizOption> options = new ArrayList<>();
}
