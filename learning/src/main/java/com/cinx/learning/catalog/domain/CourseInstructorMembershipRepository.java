package com.cinx.learning.catalog.domain;

import com.cinx.learning.shared.domain.CourseMembershipStatus;

import java.util.List;
import java.util.Optional;

/** Port: where CourseInstructorMembership rows are stored. */
public interface CourseInstructorMembershipRepository {
    Optional<CourseInstructorMembership> findByCourseIdAndInstructorId(String courseId, String instructorId);

    List<CourseInstructorMembership> findByCourseIdOrderByCreatedAtAsc(String courseId);

    List<CourseInstructorMembership> findByCourseIdAndStatusOrderByCreatedAtAsc(String courseId, CourseMembershipStatus status);

    List<CourseInstructorMembership> findByInstructorIdAndStatus(String instructorId, CourseMembershipStatus status);

    void deleteAll(Iterable<CourseInstructorMembership> entities);

    CourseInstructorMembership save(CourseInstructorMembership entity);
}
