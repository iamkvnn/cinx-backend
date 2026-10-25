package com.cinx.learning.catalog.domain;

import java.util.Optional;

/** Port: where RejectCourseReason rows are stored. */
public interface RejectCourseReasonRepository {
    Optional<RejectCourseReason> findByCourse(String courseId);

    void deleteForCourse(String courseId);

    RejectCourseReason save(RejectCourseReason entity);
}
