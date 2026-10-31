package com.cinx.learning.assessment.infrastructure.persistence;

import com.cinx.common.paging.PageQuery;
import com.cinx.common.paging.PageResult;
import com.cinx.learning.assessment.domain.QuizSessionQuestion;
import com.cinx.learning.assessment.domain.QuizSessionQuestionRepository;
import com.cinx.learning.shared.infrastructure.persistence.Pages;
import java.util.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class QuizSessionQuestionRepositoryAdapter implements QuizSessionQuestionRepository {
    private final QuizSessionQuestionJpaRepository jpa;
    private final QuizSessionQuestionPersistenceMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public PageResult<QuizSessionQuestion> findAllByQuizSessionId(String quizSessionId, PageQuery pageable) {
        return Pages.result(jpa.findAllByQuizSessionId(quizSessionId, Pages.pageable(pageable)), pageable, mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<QuizSessionQuestion> findByQuizSessionIdAndQuestionId(String quizSessionId, String questionId) {
        return jpa.findByQuizSessionIdAndQuestionId(quizSessionId, questionId).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Object[]> getQuizAnalyticsByQuizId(String quizId) {
        return jpa.getQuizAnalyticsByQuizId(quizId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuizSessionQuestion> findAllByQuizLessonIdAndQuestionIdIn(String quizLessonId, List<String> questionIds) {
        return jpa.findAllByQuizLessonIdAndQuestionIdIn(quizLessonId, questionIds).stream().map(mapper::toDomain).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuizSessionQuestion> findAllEssayByQuizSessionId(String sessionId) {
        return jpa.findAllEssayByQuizSessionId(sessionId).stream().map(mapper::toDomain).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuizSessionQuestion> findAllByQuizSessionIdIn(Collection<String> strings) {
        return jpa.findAllByQuizSessionIdIn(strings).stream().map(mapper::toDomain).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional
    public QuizSessionQuestion save(QuizSessionQuestion entity) {
        return store(entity);
    }

    @Override
    @Transactional
    public List<QuizSessionQuestion> saveAll(Iterable<QuizSessionQuestion> entities) {
        List<QuizSessionQuestion> saved = new ArrayList<>();
        entities.forEach(entity -> saved.add(store(entity)));
        return saved;
    }

    /** Inserts a new QuizSessionQuestion, or writes the changes onto the stored row and leaves the lock version to the store. */
    private QuizSessionQuestion store(QuizSessionQuestion entity) {
        QuizSessionQuestionJpaEntity stored = entity.getId() == null ? null : jpa.findById(entity.getId()).orElse(null);
        if (stored == null) {
            return mapper.toDomain(jpa.save(mapper.toEntity(entity)));
        }
        mapper.update(stored, entity);
        return mapper.toDomain(jpa.save(stored));
    }
}
