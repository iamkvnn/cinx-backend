package com.cinx.learning.assessment.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Port: where QuizSessionSubmission rows are stored. */
public interface QuizSessionSubmissionRepository {
    Optional<QuizSessionSubmission> findByQuizSessionId(String quizSessionId);

    List<Double> findScoresByUserIdAndQuizLessonId(String userId, String quizLessonId);

    List<QuizSessionSubmission> findAllByQuizLessonId(String quizLessonId);

    List<QuizSessionSubmission> findAllByQuizSessionIdIn(Collection<String> strings);

    QuizSessionSubmission save(QuizSessionSubmission entity);
}
