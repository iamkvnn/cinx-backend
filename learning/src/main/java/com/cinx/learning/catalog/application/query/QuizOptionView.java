package com.cinx.learning.catalog.application.query;

public record QuizOptionView (
        String id,
        String optionText,
        Boolean isCorrect,
        Integer optionOrder,
        String matchText
) {}

