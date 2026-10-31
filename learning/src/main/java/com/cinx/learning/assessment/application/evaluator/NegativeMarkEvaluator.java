package com.cinx.learning.assessment.application.evaluator;

import com.cinx.learning.shared.domain.QuizQuestionType;
import com.cinx.learning.assessment.domain.QuizSessionQuestion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * NEGATIVE_MARK evaluator — awards partial credit for correct selections but penalizes wrong ones.
 *
 * <p>Formula: score = (correctly_selected - incorrectly_selected) / total_correct, clamped to [0, 1].
 *
 * <p>For MATCHING: a wrong pair = any user pair whose (optionId, matchText) combination
 * is not in the correct set.
 */
@Component
@RequiredArgsConstructor
public class NegativeMarkEvaluator implements IQuestionEvaluator {
    private final AnswerParser answerParser;

    @Override
    public double evaluate(QuizSessionQuestion question) {
        if (question.getQuestionType() == QuizQuestionType.MATCHING) {
            List<AnswerParser.MatchingPair> correct = answerParser.parseMatchingPairs(question.getCorrectAnswer());
            List<AnswerParser.MatchingPair> user    = answerParser.parseMatchingPairs(question.getUserAnswer());
            return calculatePairScore(correct, user);
        }
        List<String> correct = answerParser.parseStringList(question.getCorrectAnswer());
        List<String> user    = answerParser.parseStringList(question.getUserAnswer());
        return calculateScore(correct, user);
    }

    private double calculateScore(List<String> correct, List<String> user) {
        if (correct.isEmpty()) return 1.0;
        if (user.isEmpty())    return 0.0;
        Set<String> correctSet = new HashSet<>(correct);
        long correctSelections   = user.stream().filter(correctSet::contains).count();
        long incorrectSelections = user.stream().filter(t -> !correctSet.contains(t)).count();
        double raw = ((double) correctSelections - incorrectSelections) / correct.size();
        return Math.max(0.0, Math.min(1.0, raw));
    }

    private double calculatePairScore(List<AnswerParser.MatchingPair> correct, List<AnswerParser.MatchingPair> user) {
        if (correct.isEmpty()) return 1.0;
        if (user.isEmpty())    return 0.0;
        Set<AnswerParser.MatchingPair> correctSet = new HashSet<>(correct);
        long correctSel   = user.stream().filter(correctSet::contains).count();
        long incorrectSel = user.stream().filter(p -> !correctSet.contains(p)).count();
        double raw = ((double) correctSel - incorrectSel) / correct.size();
        return Math.max(0.0, Math.min(1.0, raw));
    }
}
