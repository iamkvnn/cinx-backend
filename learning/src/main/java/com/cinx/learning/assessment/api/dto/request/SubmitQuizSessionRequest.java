package com.cinx.learning.assessment.api.dto.request;

import jakarta.validation.Valid;

import java.util.List;

public record SubmitQuizSessionRequest(
    @Valid
    List<ChooseQuizAnswerRequest> answers
) {
}
