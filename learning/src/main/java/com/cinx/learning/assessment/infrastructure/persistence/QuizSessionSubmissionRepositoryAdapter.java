package com.cinx.learning.assessment.infrastructure.persistence;

import com.cinx.learning.assessment.domain.QuizSessionSubmission;
import com.cinx.learning.assessment.domain.QuizSessionSubmissionRepository;
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
class QuizSessionSubmissionRepositoryAdapter implements QuizSessionSubmissionRepository {
    private final QuizSessionSubmissionJpaRepository jpa;
    private final QuizSessionSubmissionPersistenceMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public Optional<QuizSessionSubmission> findByQuizSessionId(String quizSessionId) {
        return jpa.findByQuizSessionId(quizSessionId).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Double> findScoresByUserIdAndQuizLessonId(String userId, String quizLessonId) {
        return jpa.findScoresByUserIdAndQuizLessonId(userId, quizLessonId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuizSessionSubmission> findAllByQuizLessonId(String quizLessonId) {
        return jpa.findAllByQuizLessonId(quizLessonId).stream().map(mapper::toDomain).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuizSessionSubmission> findAllByQuizSessionIdIn(Collection<String> strings) {
        return jpa.findAllByQuizSessionIdIn(strings).stream().map(mapper::toDomain).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional
    public QuizSessionSubmission save(QuizSessionSubmission entity) {
        return store(entity);
    }

    /** Inserts a new QuizSessionSubmission, or writes the changes onto the stored row and leaves the lock version to the store. */
    private QuizSessionSubmission store(QuizSessionSubmission entity) {
        QuizSessionSubmissionJpaEntity stored = entity.getId() == null ? null : jpa.findById(entity.getId()).orElse(null);
        if (stored == null) {
            return mapper.toDomain(jpa.save(mapper.toEntity(entity)));
        }
        mapper.update(stored, entity);
        return mapper.toDomain(jpa.save(stored));
    }
}
