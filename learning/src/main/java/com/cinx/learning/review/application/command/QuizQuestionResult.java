package com.cinx.learning.review.application.command;

/**
 * One graded question of an official quiz attempt.
 *
 * @param score 0 to 1 from the quiz's evaluator; below 1 sends the question to review
 */
public record QuizQuestionResult(String questionId, double score) {
}
