package com.cinx.learning.catalog.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Port: where QuizQuestion rows are stored. */
public interface QuizQuestionRepository {
    List<QuizQuestion> findAllByQuizLessonId(String quizLessonId);

    List<QuizQuestion> findAllByQuizLessonIdAndNeedSync(String quizLessonId);

    Optional<QuizQuestion> findByIdAndQuizLessonId(String id, String quizLessonId);

    int countByQuizLessonId(String quizLessonId);

    List<QuizCount> countPerQuizLesson(java.util.Collection<String> quizLessonIds);

    void delete(QuizQuestion entity);

    void deleteAll(Iterable<QuizQuestion> entities);

    QuizQuestion save(QuizQuestion entity);

    List<QuizQuestion> saveAll(Iterable<QuizQuestion> entities);

    interface QuizCount {
            String getLessonId();
            long getTotal();
        }
}
