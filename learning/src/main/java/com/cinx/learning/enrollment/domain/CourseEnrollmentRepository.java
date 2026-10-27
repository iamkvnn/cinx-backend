package com.cinx.learning.enrollment.domain;


/** Port: where CourseEnrollment rows are stored. */
public interface CourseEnrollmentRepository {
    boolean existsByUserIdAndCourseIdAndStatus(String userId, String courseId, String status);

    long countDistinctLearners();

    CourseEnrollment save(CourseEnrollment entity);
}
