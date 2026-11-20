package com.cinx.learning.dailyquiz.application.command;

import java.util.List;

public record UpsertDailyQuizCommand(
        String topic,

        String prompt,

        String codeSnippet,

        List< Option> options,

        String answerKey,

        String explanation,

        Boolean active
) {
    public record Option(
            String key,

            String text
    ) {
    }
}
