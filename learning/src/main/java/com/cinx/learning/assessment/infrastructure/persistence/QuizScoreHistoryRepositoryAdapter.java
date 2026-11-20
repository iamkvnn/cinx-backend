package com.cinx.learning.assessment.infrastructure.persistence;

import com.cinx.learning.assessment.domain.QuizScoreHistory;
import com.cinx.learning.assessment.domain.QuizScoreHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class QuizScoreHistoryRepositoryAdapter implements QuizScoreHistoryRepository {
    private final QuizScoreHistoryJpaRepository jpa;
    private final QuizScoreHistoryPersistenceMapper mapper;

    @Override
    @Transactional
    public QuizScoreHistory save(QuizScoreHistory entity) {
        return store(entity);
    }

    /** Inserts a new QuizScoreHistory, or writes the changes onto the stored row and leaves the lock version to the store. */
    private QuizScoreHistory store(QuizScoreHistory entity) {
        QuizScoreHistoryJpaEntity stored = entity.getId() == null ? null : jpa.findById(entity.getId()).orElse(null);
        if (stored == null) {
            return mapper.toDomain(jpa.save(mapper.toEntity(entity)));
        }
        mapper.update(stored, entity);
        return mapper.toDomain(jpa.save(stored));
    }
}
