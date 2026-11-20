package com.cinx.learning.catalog.infrastructure.persistence;

import com.cinx.learning.catalog.domain.CourseDraft;
import com.cinx.learning.catalog.domain.CourseDraftRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class CourseDraftRepositoryAdapter implements CourseDraftRepository {
    private final CourseDraftJpaRepository jpa;
    private final CourseDraftPersistenceMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public Optional<CourseDraft> findByCourse(String courseId) {
        return jpa.findByCourse(courseId).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public void delete(CourseDraft entity) {
        if (entity.getId() != null) {
            jpa.deleteById(entity.getId());
        }
    }

    @Override
    @Transactional
    public CourseDraft save(CourseDraft entity) {
        return store(entity);
    }

    /** Inserts a new CourseDraft, or writes the changes onto the stored row and leaves the lock version to the store. */
    private CourseDraft store(CourseDraft entity) {
        CourseDraftJpaEntity stored = entity.getId() == null ? null : jpa.findById(entity.getId()).orElse(null);
        if (stored == null) {
            return mapper.toDomain(jpa.save(mapper.toEntity(entity)));
        }
        mapper.update(stored, entity);
        return mapper.toDomain(jpa.save(stored));
    }
}
