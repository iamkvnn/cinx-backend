package com.cinx.learning.catalog.infrastructure.persistence;

import com.cinx.learning.shared.domain.CourseMembershipStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

interface CourseInstructorMembershipJpaRepository extends JpaRepository<CourseInstructorMembershipJpaEntity, String> {
    Optional<CourseInstructorMembershipJpaEntity> findByCourseIdAndInstructorId(String courseId, String instructorId);

    List<CourseInstructorMembershipJpaEntity> findByCourseIdOrderByCreatedAtAsc(String courseId);

    List<CourseInstructorMembershipJpaEntity> findByCourseIdAndStatusOrderByCreatedAtAsc(String courseId, CourseMembershipStatus status);

    List<CourseInstructorMembershipJpaEntity> findByInstructorIdAndStatus(String instructorId, CourseMembershipStatus status);

    boolean existsByCourseIdAndInstructorIdAndStatus(String courseId, String instructorId, CourseMembershipStatus status);

    @Query("""
        SELECT m.courseId
        FROM CourseInstructorMembership m
        WHERE m.instructorId = :instructorId
          AND m.status = com.cinx.learning.shared.domain.CourseMembershipStatus.ACTIVE
    """)
    List<String> findActiveCourseIdsByInstructorId(@Param("instructorId") String instructorId);
}
