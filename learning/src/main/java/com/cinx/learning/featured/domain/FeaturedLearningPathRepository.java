package com.cinx.learning.featured.domain;

import java.util.List;
import java.util.Optional;

/** Port for the stored FeaturedLearningPath rows. */
public interface FeaturedLearningPathRepository {
    List<FeaturedLearningPath> findByActiveTrueOrderBySortOrderAsc();

    List<FeaturedLearningPath> findAllByOrderBySortOrderAsc();

    int maxSortOrder();

    List<FeaturedLearningPath> findAllContainingCourse(String courseId);

    void delete(FeaturedLearningPath entity);

    List<FeaturedLearningPath> findAll();

    Optional<FeaturedLearningPath> findById(String id);

    FeaturedLearningPath save(FeaturedLearningPath entity);

    List<FeaturedLearningPath> saveAll(Iterable<FeaturedLearningPath> entities);
}
