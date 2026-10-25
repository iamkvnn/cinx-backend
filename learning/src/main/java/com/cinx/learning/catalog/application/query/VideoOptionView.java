package com.cinx.learning.catalog.application.query;

public record VideoOptionView(
        String id,
        String optionText,
        Boolean isCorrect
) {
}
