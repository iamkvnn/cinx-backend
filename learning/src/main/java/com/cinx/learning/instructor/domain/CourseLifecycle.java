package com.cinx.learning.instructor.domain;

import com.cinx.learning.shared.domain.CoursePublishStatus;
import com.cinx.learning.shared.domain.CourseStatus;

/** One word for where a course is, derived from status + publishStatus (api-contract-courses.md). */
public enum CourseLifecycle {
    DRAFT,
    IN_REVIEW,
    NEEDS_FIX,
    LIVE,
    ARCHIVED;

    public static CourseLifecycle of(CourseStatus status, CoursePublishStatus publishStatus) {
        if (status == CourseStatus.ARCHIVED) {
            return ARCHIVED;
        }
        if (publishStatus == CoursePublishStatus.WAITING_APPROVAL) {
            return IN_REVIEW;
        }
        if (publishStatus == CoursePublishStatus.REJECTED) {
            return NEEDS_FIX;
        }
        return status == CourseStatus.PUBLISHED ? LIVE : DRAFT;
    }
}
