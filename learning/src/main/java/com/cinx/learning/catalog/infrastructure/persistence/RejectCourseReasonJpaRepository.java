package com.cinx.learning.catalog.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

interface RejectCourseReasonJpaRepository extends JpaRepository<RejectCourseReasonJpaEntity, String> {
    @Query("""
        SELECT r
        FROM RejectCourseReason r
        WHERE r.courseId = :courseId
    """)
    Optional<RejectCourseReasonJpaEntity> findByCourse(@Param("courseId") String courseId);

    @Modifying
    @Query("""
        DELETE FROM RejectCourseReason r
        WHERE r.courseId = :courseId
    """)
    void deleteForCourse(@Param("courseId") String courseId);
}

