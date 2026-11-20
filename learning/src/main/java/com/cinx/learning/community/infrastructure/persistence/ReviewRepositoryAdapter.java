package com.cinx.learning.community.infrastructure.persistence;

import com.cinx.common.paging.PageQuery;
import com.cinx.common.paging.PageResult;
import com.cinx.learning.community.domain.Review;
import com.cinx.learning.community.domain.ReviewRepository;
import com.cinx.learning.shared.infrastructure.persistence.Pages;
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
class ReviewRepositoryAdapter implements ReviewRepository {
    private final ReviewJpaRepository jpa;
    private final ReviewPersistenceMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public PageResult<Review> findByCourseId(String courseId, PageQuery pageable) {
        return Pages.result(jpa.findByCourseId(courseId, Pages.pageable(pageable)), pageable, mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Review> findUnrepliedByCourseId(String courseId, PageQuery pageable) {
        return Pages.result(jpa.findUnrepliedByCourseId(courseId, Pages.pageable(pageable)), pageable, mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UnrepliedGroup> findUnrepliedGroups(java.util.Collection<String> courseIds) {
        return jpa.findUnrepliedGroups(courseIds).stream().map(ReviewRepositoryAdapter::wrapUnrepliedGroup).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public Double averageRating(java.util.Collection<String> courseIds) {
        return jpa.averageRating(courseIds);
    }

    @Override
    @Transactional(readOnly = true)
    public Double averageRatingBefore(java.util.Collection<String> courseIds, java.time.LocalDateTime before) {
        return jpa.averageRatingBefore(courseIds, before);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByCourseIdIn(java.util.Collection<String> courseIds) {
        return jpa.countByCourseIdIn(courseIds);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByUserIdAndCourseId(String userId, String courseId) {
        return jpa.existsByUserIdAndCourseId(userId, courseId);
    }

    @Override
    @Transactional(readOnly = true)
    public Long countByCourseId(String courseId) {
        return jpa.countByCourseId(courseId);
    }

    @Override
    @Transactional(readOnly = true)
    public Double getAverageRatingByCourseId(String courseId) {
        return jpa.getAverageRatingByCourseId(courseId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Object[]> countRatingsByCourseId(String courseId) {
        return jpa.countRatingsByCourseId(courseId);
    }

    @Override
    @Transactional
    public void delete(Review entity) {
        if (entity.getId() != null) {
            jpa.deleteById(entity.getId());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(String id) {
        return jpa.existsById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Review> findAllById(Iterable<String> ids) {
        return jpa.findAllById(ids).stream().map(mapper::toDomain).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Review> findById(String id) {
        return jpa.findById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public Review save(Review entity) {
        return store(entity);
    }

    /** Inserts a new Review, or writes the changes onto the stored row and leaves the lock version to the store. */
    private Review store(Review entity) {
        ReviewJpaEntity stored = entity.getId() == null ? null : jpa.findById(entity.getId()).orElse(null);
        if (stored == null) {
            return mapper.toDomain(jpa.save(mapper.toEntity(entity)));
        }
        mapper.update(stored, entity);
        return mapper.toDomain(jpa.save(stored));
    }

    private static ReviewRepository.UnrepliedGroup wrapUnrepliedGroup(ReviewJpaRepository.UnrepliedGroup row) {
        if (row == null) {
            return null;
        }
        return new ReviewRepository.UnrepliedGroup() {
            @Override
            public String getCourseId() {
                return row.getCourseId();
            }
            @Override
            public long getWaiting() {
                return row.getWaiting();
            }
            @Override
            public java.time.LocalDateTime getOldest() {
                return row.getOldest();
            }
        };
    }
}
