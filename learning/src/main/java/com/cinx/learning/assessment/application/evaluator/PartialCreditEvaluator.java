package com.cinx.learning.assessment.application.evaluator;

import com.cinx.learning.assessment.domain.QuizSessionQuestion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * PARTIAL_CREDIT evaluator — awards a fraction of the mark equal to the proportion of correct elements matched.
 *
 * <ul>
 *   <li>MULTI_CHOICE — score = correctly selected / total correct. No penalty for wrong selections.</li>
 *   <li>ORDERING     — positional match: score = positions matched / total positions.</li>
 *   <li>MATCHING     — score = correct pairs matched / total correct pairs (set comparison).</li>
 *   <li>Others       — set intersection / total correct (fallback).</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class PartialCreditEvaluator implements IQuestionEvaluator {
    private final AnswerParser answerParser;

    @Override
    public double evaluate(QuizSessionQuestion question) {
        switch (question.getQuestionType()) {
            case ORDERING -> {
                List<String> correct = answerParser.parseStringList(question.getCorrectAnswer());
                List<String> user    = answerParser.parseStringList(question.getUserAnswer());
                if (correct.isEmpty()) return 1.0;
                if (user.isEmpty())    return 0.0;
                int matched = 0;
                int len = Math.min(correct.size(), user.size());
                for (int i = 0; i < len; i++) {
                    if (correct.get(i).equalsIgnoreCase(user.get(i))) matched++;
                }
                return (double) matched / correct.size();
            }
            case MATCHING -> {
                List<AnswerParser.MatchingPair> correct = answerParser.parseMatchingPairs(question.getCorrectAnswer());
                List<AnswerParser.MatchingPair> user    = answerParser.parseMatchingPairs(question.getUserAnswer());
                if (correct.isEmpty()) return 1.0;
                if (user.isEmpty())    return 0.0;
                Set<AnswerParser.MatchingPair> correctSet = new HashSet<>(correct);
                long matched = user.stream().filter(correctSet::contains).count();
                return (double) matched / correct.size();
            }
            default -> {
                // SINGLE_CHOICE, MULTI_CHOICE — set intersection
                List<String> correct = answerParser.parseStringList(question.getCorrectAnswer());
                List<String> user    = answerParser.parseStringList(question.getUserAnswer());
                if (correct.isEmpty()) return 1.0;
                if (user.isEmpty())    return 0.0;
                Set<String> correctSet = new HashSet<>(correct);
                long matched = user.stream().filter(correctSet::contains).count();
                return (double) matched / correct.size();
            }
        }
    }
}
