package com.cinx.learning.featured.domain;

import java.util.List;
import java.util.Optional;

/** Port for the stored FeaturedReview rows. */
public interface FeaturedReviewRepository {
    List<FeaturedReview> findByActiveTrueOrderBySortOrderAsc();

    List<FeaturedReview> findAllByOrderBySortOrderAsc();

    boolean existsByReviewId(String reviewId);

    int maxSortOrder();

    void delete(FeaturedReview entity);

    List<FeaturedReview> findAll();

    Optional<FeaturedReview> findById(String id);

    FeaturedReview save(FeaturedReview entity);

    List<FeaturedReview> saveAll(Iterable<FeaturedReview> entities);
}
