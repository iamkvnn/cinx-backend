package com.cinx.learning.catalog.infrastructure.persistence;

import com.cinx.learning.catalog.domain.QuizQuestion;
import com.cinx.learning.catalog.domain.QuizQuestionRepository;
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
class QuizQuestionRepositoryAdapter implements QuizQuestionRepository {
    private final QuizQuestionJpaRepository jpa;
    private final QuizQuestionPersistenceMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<QuizQuestion> findAllByQuizLessonId(String quizLessonId) {
        return jpa.findAllByQuizLessonId(quizLessonId).stream().map(mapper::toDomain).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuizQuestion> findAllByQuizLessonIdAndNeedSync(String quizLessonId) {
        return jpa.findAllByQuizLessonIdAndNeedSync(quizLessonId).stream().map(mapper::toDomain).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<QuizQuestion> findByIdAndQuizLessonId(String id, String quizLessonId) {
        return jpa.findByIdAndQuizLessonId(id, quizLessonId).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public int countByQuizLessonId(String quizLessonId) {
        return jpa.countByQuizLessonId(quizLessonId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuizCount> countPerQuizLesson(java.util.Collection<String> quizLessonIds) {
        return jpa.countPerQuizLesson(quizLessonIds).stream().map(QuizQuestionRepositoryAdapter::wrapQuizCount).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional
    public void delete(QuizQuestion entity) {
        if (entity.getId() != null) {
            jpa.deleteById(entity.getId());
        }
    }

    @Override
    @Transactional
    public void deleteAll(Iterable<QuizQuestion> entities) {
        entities.forEach(entity -> { if (entity.getId() != null) jpa.deleteById(entity.getId()); });
    }

    @Override
    @Transactional
    public QuizQuestion save(QuizQuestion entity) {
        return store(entity);
    }

    @Override
    @Transactional
    public List<QuizQuestion> saveAll(Iterable<QuizQuestion> entities) {
        List<QuizQuestion> saved = new ArrayList<>();
        entities.forEach(entity -> saved.add(store(entity)));
        return saved;
    }

    /** Inserts a new QuizQuestion, or writes the changes onto the stored row and leaves the lock version to the store. */
    private QuizQuestion store(QuizQuestion entity) {
        QuizQuestionJpaEntity stored = entity.getId() == null ? null : jpa.findById(entity.getId()).orElse(null);
        if (stored == null) {
            return mapper.toDomain(jpa.save(mapper.toEntity(entity)));
        }
        mapper.update(stored, entity);
        return mapper.toDomain(jpa.save(stored));
    }

    private static QuizQuestionRepository.QuizCount wrapQuizCount(QuizQuestionJpaRepository.QuizCount row) {
        if (row == null) {
            return null;
        }
        return new QuizQuestionRepository.QuizCount() {
            @Override
            public String getLessonId() {
                return row.getLessonId();
            }
            @Override
            public long getTotal() {
                return row.getTotal();
            }
        };
    }
}
