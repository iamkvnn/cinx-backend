package com.cinx.learning.catalog.infrastructure.persistence;

import com.cinx.learning.catalog.domain.QuizLesson;
import com.cinx.learning.catalog.domain.QuizLessonRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class QuizLessonRepositoryAdapter implements QuizLessonRepository {
    private final QuizLessonJpaRepository jpa;
    private final QuizLessonPersistenceMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public Optional<QuizLesson> findByLessonId(String lessonId) {
        return jpa.findByLessonId(lessonId).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public void delete(QuizLesson entity) {
        if (entity.getLessonId() != null) {
            jpa.deleteById(entity.getLessonId());
        }
    }

    @Override
    @Transactional
    public QuizLesson save(QuizLesson entity) {
        return store(entity);
    }

    /** Inserts a new QuizLesson, or writes the changes onto the stored row and leaves the lock version to the store. */
    private QuizLesson store(QuizLesson entity) {
        QuizLessonJpaEntity stored = entity.getLessonId() == null ? null : jpa.findById(entity.getLessonId()).orElse(null);
        if (stored == null) {
            return mapper.toDomain(jpa.save(mapper.toEntity(entity)));
        }
        mapper.update(stored, entity);
        return mapper.toDomain(jpa.save(stored));
    }
}
