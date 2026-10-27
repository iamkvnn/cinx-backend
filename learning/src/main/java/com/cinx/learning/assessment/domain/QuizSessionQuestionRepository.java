package com.cinx.learning.assessment.domain;

import com.cinx.common.paging.PageQuery;
import com.cinx.common.paging.PageResult;
import java.util.*;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Port: where QuizSessionQuestion rows are stored. */
public interface QuizSessionQuestionRepository {
    PageResult<QuizSessionQuestion> findAllByQuizSessionId(String quizSessionId, PageQuery pageable);

    Optional<QuizSessionQuestion> findByQuizSessionIdAndQuestionId(String quizSessionId, String questionId);

    List<Object[]> getQuizAnalyticsByQuizId(String quizId);

    List<QuizSessionQuestion> findAllByQuizLessonIdAndQuestionIdIn(String quizLessonId, List<String> questionIds);

    List<QuizSessionQuestion> findAllEssayByQuizSessionId(String sessionId);

    List<QuizSessionQuestion> findAllByQuizSessionIdIn(Collection<String> strings);

    QuizSessionQuestion save(QuizSessionQuestion entity);

    List<QuizSessionQuestion> saveAll(Iterable<QuizSessionQuestion> entities);
}
