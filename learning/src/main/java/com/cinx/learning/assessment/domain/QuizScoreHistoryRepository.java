package com.cinx.learning.assessment.domain;


/** Port: where QuizScoreHistory rows are stored. */
public interface QuizScoreHistoryRepository {
    QuizScoreHistory save(QuizScoreHistory entity);
}
