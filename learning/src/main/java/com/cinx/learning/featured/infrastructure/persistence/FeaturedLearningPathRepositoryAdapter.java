package com.cinx.learning.featured.infrastructure.persistence;

import com.cinx.learning.featured.domain.FeaturedLearningPath;
import com.cinx.learning.featured.domain.FeaturedLearningPathRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class FeaturedLearningPathRepositoryAdapter implements FeaturedLearningPathRepository {
    private final FeaturedLearningPathJpaRepository jpa;
    private final FeaturedLearningPathPersistenceMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<FeaturedLearningPath> findByActiveTrueOrderBySortOrderAsc() {
        return jpa.findByActiveTrueOrderBySortOrderAsc().stream().map(mapper::toDomain).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public List<FeaturedLearningPath> findAllByOrderBySortOrderAsc() {
        return jpa.findAllByOrderBySortOrderAsc().stream().map(mapper::toDomain).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public int maxSortOrder() {
        return jpa.maxSortOrder();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FeaturedLearningPath> findAllContainingCourse(String courseId) {
        return jpa.findAllContainingCourse(courseId).stream().map(mapper::toDomain).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional
    public void delete(FeaturedLearningPath entity) {
        if (entity.getId() != null) {
            jpa.deleteById(entity.getId());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<FeaturedLearningPath> findAll() {
        return jpa.findAll().stream().map(mapper::toDomain).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FeaturedLearningPath> findById(String id) {
        return jpa.findById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public FeaturedLearningPath save(FeaturedLearningPath entity) {
        return store(entity);
    }

    @Override
    @Transactional
    public List<FeaturedLearningPath> saveAll(Iterable<FeaturedLearningPath> entities) {
        List<FeaturedLearningPath> saved = new ArrayList<>();
        entities.forEach(entity -> saved.add(store(entity)));
        return saved;
    }

    /** Inserts a new FeaturedLearningPath, or writes the changes onto the stored row and leaves the lock version to the store. */
    private FeaturedLearningPath store(FeaturedLearningPath entity) {
        FeaturedLearningPathJpaEntity stored = entity.getId() == null ? null : jpa.findById(entity.getId()).orElse(null);
        if (stored == null) {
            return mapper.toDomain(jpa.save(mapper.toEntity(entity)));
        }
        mapper.update(stored, entity);
        return mapper.toDomain(jpa.save(stored));
    }
}
