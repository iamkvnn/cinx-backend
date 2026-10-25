package com.cinx.learning.catalog.application.query;

import com.cinx.learning.catalog.domain.QuizQuestionType;
import com.cinx.learning.catalog.domain.ScoringMethod;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class QuizQuestionViewTest {
    private static QuizQuestionView essayWithExplanation() {
        return new QuizQuestionView("q-1", "Explain caching.", QuizQuestionType.ESSAY, ScoringMethod.ALL_OR_NOTHING,
                false, List.of(), "Mention TTL.", "A cache trades freshness for speed.", true);
    }

    @Test
    void beforeAnAttemptALearnerGetsNeitherKeyNorExplanation() {
        QuizQuestionView learner = essayWithExplanation().withoutAnswerKeys();

        assertThat(learner.referenceAnswer()).isNull();
        assertThat(learner.explanation()).isNull();
    }

    @Test
    void theSessionCopyKeepsTheExplanationForReviewButNotTheReferenceAnswer() {
        QuizQuestionView session = essayWithExplanation().withoutReferenceAnswer();

        assertThat(session.referenceAnswer()).isNull();
        assertThat(session.explanation()).isEqualTo("A cache trades freshness for speed.");
    }

    @Test
    void theAiLabelSurvivesBothLearnerCopies() {
        assertThat(essayWithExplanation().withoutAnswerKeys().aiAssisted()).isTrue();
        assertThat(essayWithExplanation().withoutReferenceAnswer().aiAssisted()).isTrue();
    }
}
