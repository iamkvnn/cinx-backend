package com.cinx.learning.featured.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

interface FeaturedCourseJpaRepository extends JpaRepository<FeaturedCourseJpaEntity, String> {
    List<FeaturedCourseJpaEntity> findByActiveTrueOrderBySortOrderAsc();

    List<FeaturedCourseJpaEntity> findAllByOrderBySortOrderAsc();

    boolean existsByCourseId(String courseId);

    List<FeaturedCourseJpaEntity> findAllByCourseId(String courseId);

    @Query("SELECT COALESCE(MAX(f.sortOrder), -1) FROM FeaturedCourse f")
    int maxSortOrder();
}
