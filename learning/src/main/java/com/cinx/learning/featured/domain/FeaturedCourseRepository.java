package com.cinx.learning.featured.domain;

import java.util.List;
import java.util.Optional;

/** Port for the stored FeaturedCourse rows. */
public interface FeaturedCourseRepository {
    List<FeaturedCourse> findByActiveTrueOrderBySortOrderAsc();

    List<FeaturedCourse> findAllByOrderBySortOrderAsc();

    boolean existsByCourseId(String courseId);

    List<FeaturedCourse> findAllByCourseId(String courseId);

    int maxSortOrder();

    void delete(FeaturedCourse entity);

    void deleteAll(Iterable<FeaturedCourse> entities);

    List<FeaturedCourse> findAll();

    Optional<FeaturedCourse> findById(String id);

    FeaturedCourse save(FeaturedCourse entity);

    List<FeaturedCourse> saveAll(Iterable<FeaturedCourse> entities);
}
