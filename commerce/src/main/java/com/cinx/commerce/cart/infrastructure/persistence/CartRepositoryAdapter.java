package com.cinx.commerce.cart.infrastructure.persistence;

import com.cinx.commerce.cart.domain.CartItem;
import com.cinx.commerce.cart.domain.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
class CartRepositoryAdapter implements CartRepository {
    private final CartItemJpaRepository jpa;
    private final CartItemPersistenceMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<CartItem> findAllByUserId(String userId) {
        return jpa.findAllByUserIdOrderByAddedAtDesc(userId).stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByUserIdAndCourseId(String userId, String courseId) {
        return jpa.existsByUserIdAndCourseId(userId, courseId);
    }

    @Override
    @Transactional
    public CartItem save(CartItem item) {
        return mapper.toDomain(jpa.save(mapper.toEntity(item)));
    }

    @Override
    @Transactional
    public boolean deleteByUserIdAndCourseId(String userId, String courseId) {
        return jpa.deleteByUserIdAndCourseId(userId, courseId) > 0;
    }

    @Override
    @Transactional
    public void deleteAllByUserId(String userId) {
        jpa.deleteAllByUserId(userId);
    }
}
