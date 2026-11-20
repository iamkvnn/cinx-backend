package com.cinx.learning.catalog.api.dto.request;

import com.cinx.learning.catalog.domain.QuizQuestionType;
import com.cinx.learning.catalog.domain.ScoringMethod;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class QuizQuestionRequestValidationTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void aQuestionMayHoldTwoThousandCharactersAndNoMore() {
        assertThat(violations(create("x".repeat(2000)))).doesNotContain("questionText");
        assertThat(violations(create("x".repeat(2001)))).contains("questionText");
        assertThat(violations(update("x".repeat(2001)))).contains("questionText");
    }

    private List<String> violations(Object request) {
        return validator.validate(request).stream().map(v -> v.getPropertyPath().toString()).toList();
    }

    private static CreateQuizQuestionRequest create(String text) {
        return new CreateQuizQuestionRequest(text, QuizQuestionType.ESSAY, ScoringMethod.ALL_OR_NOTHING, null, null, null, null);
    }

    private static UpdateQuizQuestionRequest update(String text) {
        return new UpdateQuizQuestionRequest(text, ScoringMethod.ALL_OR_NOTHING, null, null, null);
    }
}
