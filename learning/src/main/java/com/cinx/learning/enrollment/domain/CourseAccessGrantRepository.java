package com.cinx.learning.enrollment.domain;

import java.time.LocalDateTime;

/** Port: where CourseAccessGrant rows are stored. */
public interface CourseAccessGrantRepository {
    boolean existsActive(String userId, String courseId, String status, LocalDateTime now);

    boolean existsByUserIdAndCourseIdAndSourceAndSourceReference(String userId, String courseId, String source, String sourceReference);

    CourseAccessGrant save(CourseAccessGrant entity);
}
