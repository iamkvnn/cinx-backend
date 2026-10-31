package com.cinx.learning.assessment.application.evaluator;

import com.cinx.learning.assessment.domain.QuizSessionQuestion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * SHORT_TEXT evaluator — accepts any one of the pre-defined acceptable answers.
 *
 * <p>Correct answer JSON: {@code ["Paris","paris","city of light"]} (multiple accepted forms).
 * User answer JSON: {@code ["paris"]} (single element typed by student).
 *
 * <p>Matching is case-insensitive and trims surrounding whitespace.
 * Returns 1.0 if the user's answer matches ANY accepted answer, 0.0 otherwise.
 */
@Component
@RequiredArgsConstructor
public class ShortTextEvaluator implements IQuestionEvaluator {
    private final AnswerParser answerParser;

    @Override
    public double evaluate(QuizSessionQuestion question) {
        List<String> accepted = answerParser.parseStringList(question.getCorrectAnswer());
        List<String> userList = answerParser.parseStringList(question.getUserAnswer());

        if (accepted.isEmpty()) return 1.0;
        if (userList.isEmpty()) return 0.0;

        String userInput = userList.getFirst().trim().toLowerCase();
        Set<String> acceptedNormalized = accepted.stream()
                .map(s -> s.trim().toLowerCase())
                .collect(Collectors.toSet());

        return acceptedNormalized.contains(userInput) ? 1.0 : 0.0;
    }
}
