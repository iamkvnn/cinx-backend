package com.cinx.learning.featured.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

interface FeaturedReviewJpaRepository extends JpaRepository<FeaturedReviewJpaEntity, String> {
    List<FeaturedReviewJpaEntity> findByActiveTrueOrderBySortOrderAsc();

    List<FeaturedReviewJpaEntity> findAllByOrderBySortOrderAsc();

    boolean existsByReviewId(String reviewId);

    @Query("SELECT COALESCE(MAX(f.sortOrder), -1) FROM FeaturedReview f")
    int maxSortOrder();
}
