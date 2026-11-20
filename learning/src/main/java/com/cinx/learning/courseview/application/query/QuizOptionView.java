package com.cinx.learning.courseview.application.query;

public record QuizOptionView(
        String id,
        String optionText,
        Boolean isCorrect,
        Integer optionOrder,
        String matchText,
        String side
) {}
