package com.cinx.learning.assessment.application.evaluator;

import com.cinx.learning.shared.domain.QuizQuestionType;
import com.cinx.learning.assessment.domain.QuizSessionQuestion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;

/**
 * ALL_OR_NOTHING evaluator.
 *
 * <ul>
 *   <li>MATCHING  — all pairs must be correct (set equality, order-insensitive).</li>
 *   <li>ORDERING  — full positional sequence must match exactly.</li>
 *   <li>Others    — unordered set of token IDs must match exactly.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class AllOrNothingEvaluator implements IQuestionEvaluator {
    private final AnswerParser answerParser;

    @Override
    public double evaluate(QuizSessionQuestion question) {
        if (question.getQuestionType() == QuizQuestionType.MATCHING) {
            List<AnswerParser.MatchingPair> correct = answerParser.parseMatchingPairs(question.getCorrectAnswer());
            List<AnswerParser.MatchingPair> user    = answerParser.parseMatchingPairs(question.getUserAnswer());
            if (correct.isEmpty() && user.isEmpty()) return 1.0;
            if (correct.size() != user.size()) return 0.0;
            return new HashSet<>(correct).equals(new HashSet<>(user)) ? 1.0 : 0.0;
        }

        if (question.getQuestionType() == QuizQuestionType.ORDERING) {
            List<String> correct = answerParser.parseStringList(question.getCorrectAnswer());
            List<String> user    = answerParser.parseStringList(question.getUserAnswer());
            return correct.equals(user) ? 1.0 : 0.0;
        }

        List<String> correct = answerParser.parseStringList(question.getCorrectAnswer());
        List<String> user    = answerParser.parseStringList(question.getUserAnswer());
        if (correct.isEmpty() && user.isEmpty()) return 1.0;
        if (correct.size() != user.size()) return 0.0;
        List<String> sortedCorrect = correct.stream().sorted().toList();
        List<String> sortedUser    = user.stream().sorted().toList();
        return sortedCorrect.equals(sortedUser) ? 1.0 : 0.0;
    }
}
