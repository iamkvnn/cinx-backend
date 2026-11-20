package com.cinx.learning.featured.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

interface FeaturedLearningPathJpaRepository extends JpaRepository<FeaturedLearningPathJpaEntity, String> {
    List<FeaturedLearningPathJpaEntity> findByActiveTrueOrderBySortOrderAsc();

    List<FeaturedLearningPathJpaEntity> findAllByOrderBySortOrderAsc();

    @Query("SELECT COALESCE(MAX(p.sortOrder), -1) FROM FeaturedLearningPath p")
    int maxSortOrder();

    @Query("SELECT p FROM FeaturedLearningPath p WHERE :courseId MEMBER OF p.courseIds")
    List<FeaturedLearningPathJpaEntity> findAllContainingCourse(@Param("courseId") String courseId);
}
