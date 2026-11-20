package com.cinx.commerce.cart.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface CartItemJpaRepository extends JpaRepository<CartItemJpaEntity, String> {
    List<CartItemJpaEntity> findAllByUserIdOrderByAddedAtDesc(String userId);

    boolean existsByUserIdAndCourseId(String userId, String courseId);

    long deleteByUserIdAndCourseId(String userId, String courseId);

    long deleteAllByUserId(String userId);
}
