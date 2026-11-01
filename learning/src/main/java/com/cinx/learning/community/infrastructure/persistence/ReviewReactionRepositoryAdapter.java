package com.cinx.learning.community.infrastructure.persistence;

import com.cinx.learning.community.domain.ReviewReaction;
import com.cinx.learning.community.domain.ReviewReactionRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class ReviewReactionRepositoryAdapter implements ReviewReactionRepository {
    private final ReviewReactionJpaRepository jpa;
    private final ReviewReactionPersistenceMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public Optional<ReviewReaction> findByUserIdAndReviewId(String userId, String reviewId) {
        return jpa.findByUserIdAndReviewId(userId, reviewId).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewReaction> findByReviewId(String reviewId) {
        return jpa.findByReviewId(reviewId).stream().map(mapper::toDomain).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewReaction> findByReviewIdIn(List<String> reviewIds) {
        return jpa.findByReviewIdIn(reviewIds).stream().map(mapper::toDomain).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional
    public void deleteByReviewId(String reviewId) {
        jpa.deleteByReviewId(reviewId);
    }

    @Override
    @Transactional
    public ReviewReaction save(ReviewReaction entity) {
        return store(entity);
    }

    /** Inserts a new ReviewReaction, or writes the changes onto the stored row and leaves the lock version to the store. */
    private ReviewReaction store(ReviewReaction entity) {
        ReviewReactionJpaEntity stored = entity.getId() == null ? null : jpa.findById(entity.getId()).orElse(null);
        if (stored == null) {
            return mapper.toDomain(jpa.save(mapper.toEntity(entity)));
        }
        mapper.update(stored, entity);
        return mapper.toDomain(jpa.save(stored));
    }
}
