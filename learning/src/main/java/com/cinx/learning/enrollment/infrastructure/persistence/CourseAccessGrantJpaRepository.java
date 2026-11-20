package com.cinx.learning.enrollment.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
interface CourseAccessGrantJpaRepository extends JpaRepository<CourseAccessGrantJpaEntity, String> {
    @Query("select count(g) > 0 from CourseAccessGrant g where g.userId = :userId and g.courseId = :courseId and g.status = :status and g.validFrom <= :now and (g.validUntil is null or g.validUntil > :now)")
    boolean existsActive(@Param("userId") String userId, @Param("courseId") String courseId, @Param("status") String status, @Param("now") LocalDateTime now);
    boolean existsByUserIdAndCourseIdAndSourceAndSourceReference(String userId, String courseId, String source, String sourceReference);
}
