package com.cinx.learning.featured.infrastructure.persistence;

import com.cinx.learning.featured.domain.FeaturedReview;
import com.cinx.learning.featured.domain.FeaturedReviewRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class FeaturedReviewRepositoryAdapter implements FeaturedReviewRepository {
    private final FeaturedReviewJpaRepository jpa;
    private final FeaturedReviewPersistenceMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<FeaturedReview> findByActiveTrueOrderBySortOrderAsc() {
        return jpa.findByActiveTrueOrderBySortOrderAsc().stream().map(mapper::toDomain).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public List<FeaturedReview> findAllByOrderBySortOrderAsc() {
        return jpa.findAllByOrderBySortOrderAsc().stream().map(mapper::toDomain).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByReviewId(String reviewId) {
        return jpa.existsByReviewId(reviewId);
    }

    @Override
    @Transactional(readOnly = true)
    public int maxSortOrder() {
        return jpa.maxSortOrder();
    }

    @Override
    @Transactional
    public void delete(FeaturedReview entity) {
        if (entity.getId() != null) {
            jpa.deleteById(entity.getId());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<FeaturedReview> findAll() {
        return jpa.findAll().stream().map(mapper::toDomain).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FeaturedReview> findById(String id) {
        return jpa.findById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public FeaturedReview save(FeaturedReview entity) {
        return store(entity);
    }

    @Override
    @Transactional
    public List<FeaturedReview> saveAll(Iterable<FeaturedReview> entities) {
        List<FeaturedReview> saved = new ArrayList<>();
        entities.forEach(entity -> saved.add(store(entity)));
        return saved;
    }

    /** Inserts a new FeaturedReview, or writes the changes onto the stored row and leaves the lock version to the store. */
    private FeaturedReview store(FeaturedReview entity) {
        FeaturedReviewJpaEntity stored = entity.getId() == null ? null : jpa.findById(entity.getId()).orElse(null);
        if (stored == null) {
            return mapper.toDomain(jpa.save(mapper.toEntity(entity)));
        }
        mapper.update(stored, entity);
        return mapper.toDomain(jpa.save(stored));
    }
}
