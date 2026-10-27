package com.cinx.learning.assessment.application.query;

/**
 * A practice answer graded by the quiz's own evaluators.
 *
 * @param score         0 to 1, as a quiz session scores the question
 * @param correctAnswer the key in the session snapshot's format, which the client already renders on quiz review
 */
public record PracticeCheckView(double score, String correctAnswer, String explanation) {
    public boolean correct() {
        return score >= 1.0;
    }
}
