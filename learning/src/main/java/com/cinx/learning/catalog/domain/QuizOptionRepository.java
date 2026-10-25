package com.cinx.learning.catalog.domain;

import java.util.List;

/** Port: where QuizOption rows are stored. */
public interface QuizOptionRepository {
    void deleteAllByQuizQuestionId(String quizQuestionId);

    void deleteAllById(Iterable<String> ids);

    List<QuizOption> saveAll(Iterable<QuizOption> entities);
}
