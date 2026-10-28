package com.cinx.learning.catalog.infrastructure.persistence;

import com.cinx.learning.catalog.domain.QuizOption;
import com.cinx.learning.catalog.domain.QuizOptionRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class QuizOptionRepositoryAdapter implements QuizOptionRepository {
    private final QuizOptionJpaRepository jpa;
    private final QuizOptionPersistenceMapper mapper;

    @Override
    @Transactional
    public void deleteAllByQuizQuestionId(String quizQuestionId) {
        jpa.deleteAllByQuizQuestionId(quizQuestionId);
    }

    @Override
    @Transactional
    public void deleteAllById(Iterable<String> ids) {
        jpa.deleteAllById(ids);
    }

    @Override
    @Transactional
    public List<QuizOption> saveAll(Iterable<QuizOption> entities) {
        List<QuizOption> saved = new ArrayList<>();
        entities.forEach(entity -> saved.add(store(entity)));
        return saved;
    }

    /** Inserts a new QuizOption, or writes the changes onto the stored row and leaves the lock version to the store. */
    private QuizOption store(QuizOption entity) {
        QuizOptionJpaEntity stored = entity.getId() == null ? null : jpa.findById(entity.getId()).orElse(null);
        if (stored == null) {
            return mapper.toDomain(jpa.save(mapper.toEntity(entity)));
        }
        mapper.update(stored, entity);
        return mapper.toDomain(jpa.save(stored));
    }
}
