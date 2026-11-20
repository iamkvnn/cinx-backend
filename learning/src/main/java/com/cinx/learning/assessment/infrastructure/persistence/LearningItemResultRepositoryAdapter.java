package com.cinx.learning.assessment.infrastructure.persistence;

import com.cinx.learning.assessment.domain.LearningItemResult;
import com.cinx.learning.assessment.domain.LearningItemResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class LearningItemResultRepositoryAdapter implements LearningItemResultRepository {
    private final LearningItemResultJpaRepository jpa;
    private final LearningItemResultPersistenceMapper mapper;

    /** Inserts a new LearningItemResult, or writes the changes onto the stored row and leaves the lock version to the store. */
    private LearningItemResult store(LearningItemResult entity) {
        LearningItemResultJpaEntity stored = entity.getItemId() == null ? null : jpa.findById(entity.getItemId()).orElse(null);
        if (stored == null) {
            return mapper.toDomain(jpa.save(mapper.toEntity(entity)));
        }
        mapper.update(stored, entity);
        return mapper.toDomain(jpa.save(stored));
    }
}
