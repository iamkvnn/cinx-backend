package com.cinx.learning.community.infrastructure.persistence;

import com.cinx.learning.community.domain.ReviewReply;
import com.cinx.learning.community.domain.ReviewReplyRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class ReviewReplyRepositoryAdapter implements ReviewReplyRepository {
    private final ReviewReplyJpaRepository jpa;
    private final ReviewReplyPersistenceMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public Optional<ReviewReply> findByReviewId(String reviewId) {
        return jpa.findByReviewId(reviewId).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewReply> findByReviewIdIn(List<String> reviewIds) {
        return jpa.findByReviewIdIn(reviewIds).stream().map(mapper::toDomain).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional
    public void deleteByReviewId(String reviewId) {
        jpa.deleteByReviewId(reviewId);
    }

    @Override
    @Transactional(readOnly = true)
    public Long countRepliesByCourseId(String courseId) {
        return jpa.countRepliesByCourseId(courseId);
    }

    @Override
    @Transactional
    public void delete(ReviewReply entity) {
        if (entity.getId() != null) {
            jpa.deleteById(entity.getId());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ReviewReply> findById(String id) {
        return jpa.findById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public ReviewReply save(ReviewReply entity) {
        return store(entity);
    }

    /** Inserts a new ReviewReply, or writes the changes onto the stored row and leaves the lock version to the store. */
    private ReviewReply store(ReviewReply entity) {
        ReviewReplyJpaEntity stored = entity.getId() == null ? null : jpa.findById(entity.getId()).orElse(null);
        if (stored == null) {
            return mapper.toDomain(jpa.save(mapper.toEntity(entity)));
        }
        mapper.update(stored, entity);
        return mapper.toDomain(jpa.save(stored));
    }
}
