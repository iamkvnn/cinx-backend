package com.cinx.learning.enrollment.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface CourseEnrollmentJpaRepository extends JpaRepository<CourseEnrollmentJpaEntity, String> {
    boolean existsByUserIdAndCourseIdAndStatus(String userId, String courseId, String status);

    @Query("SELECT COUNT(DISTINCT e.userId) FROM CourseEnrollment e")
    long countDistinctLearners();
}
