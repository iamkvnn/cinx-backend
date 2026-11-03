package com.cinx.commerce.wishlist.infrastructure.persistence;

import com.cinx.commerce.wishlist.domain.WishlistItem;
import com.cinx.commerce.wishlist.domain.WishlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
class WishlistRepositoryAdapter implements WishlistRepository {
    private final WishlistItemJpaRepository jpa;
    private final WishlistItemPersistenceMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public boolean existsByUserIdAndCourseId(String userId, String courseId) {
        return jpa.existsByUserIdAndCourseId(userId, courseId);
    }

    @Override
    @Transactional
    public WishlistItem save(WishlistItem item) {
        return mapper.toDomain(jpa.save(mapper.toEntity(item)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<WishlistItem> findAllByUserId(String userId) {
        return jpa.findAllByUserIdOrderByAddedAtDesc(userId).stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional
    public boolean deleteByUserIdAndCourseId(String userId, String courseId) {
        return jpa.deleteByUserIdAndCourseId(userId, courseId) > 0;
    }
}
