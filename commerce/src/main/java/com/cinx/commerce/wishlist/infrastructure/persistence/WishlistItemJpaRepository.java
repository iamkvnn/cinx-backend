package com.cinx.commerce.wishlist.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface WishlistItemJpaRepository extends JpaRepository<WishlistItemJpaEntity, String> {
    List<WishlistItemJpaEntity> findAllByUserIdOrderByAddedAtDesc(String userId);

    boolean existsByUserIdAndCourseId(String userId, String courseId);

    long deleteByUserIdAndCourseId(String userId, String courseId);
}
