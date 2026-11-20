package com.cinx.learning.dailyquiz.application.query;

import java.time.LocalDate;
import java.util.List;

public record DailyQuizView(
        String id,
        String topic,
        String prompt,
        String codeSnippet,
        List<Option> options,
        String answerKey,
        String explanation,
        Boolean active,
        Integer sortOrder,
        LocalDate day
) {
    public record Option(
            String key,
            String text
    ) {
    }
}
