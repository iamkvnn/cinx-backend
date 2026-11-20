package com.cinx.learning.learningpath.infrastructure.persistence;

import com.cinx.learning.learningpath.domain.LearningPathItem;
import com.cinx.learning.learningpath.domain.LearningPathItemRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class LearningPathItemRepositoryAdapter implements LearningPathItemRepository {
    private final LearningPathItemJpaRepository jpa;
    private final LearningPathItemPersistenceMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public Optional<LearningPathItem> findByLearningPathIdAndLessonId(String learningPathId, String lessonId) {
        return jpa.findByLearningPathIdAndLessonId(learningPathId, lessonId).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public LearningPathItem save(LearningPathItem entity) {
        return store(entity);
    }

    @Override
    @Transactional
    public List<LearningPathItem> saveAll(Iterable<LearningPathItem> entities) {
        List<LearningPathItem> saved = new ArrayList<>();
        entities.forEach(entity -> saved.add(store(entity)));
        return saved;
    }

    /** Inserts a new LearningPathItem, or writes the changes onto the stored row and leaves the lock version to the store. */
    private LearningPathItem store(LearningPathItem entity) {
        LearningPathItemJpaEntity stored = entity.getId() == null ? null : jpa.findById(entity.getId()).orElse(null);
        if (stored == null) {
            return mapper.toDomain(jpa.save(mapper.toEntity(entity)));
        }
        mapper.update(stored, entity);
        return mapper.toDomain(jpa.save(stored));
    }
}
