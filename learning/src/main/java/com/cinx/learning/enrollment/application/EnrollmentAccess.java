package com.cinx.learning.enrollment.application;

import com.cinx.common.dto.ApiResponse;
import com.cinx.learning.enrollment.application.query.CheckEnrollmentStatus;
import com.cinx.learning.enrollment.application.query.CourseEntitlementInfo;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/** What the other features read of who may learn which course. */
public interface EnrollmentAccess {
    /** A permanent access grant that is active at {@code now}. */
    boolean hasActiveGrant(String userId, String courseId, LocalDateTime now);

    boolean isEntitled(String userId, String courseId);

    boolean hasActiveEnrollment(String userId, String courseId);

    boolean courseHasEntitlements(String courseId);

    Set<String> entitledCourseIds(String userId, Collection<String> courseIds);

    /** For each given course, whether the learner is entitled to it. */
    ApiResponse<List<CheckEnrollmentStatus>> checkEnrollmentStatus(String userId, List<String> courseIds);

    List<String> userIdsOfCourse(String courseId);

    List<CourseEntitlementInfo> entitlementsOfCourse(String courseId);

    long countDistinctLearners();

    List<CourseCount> countPerCourse(Collection<String> courseIds);

    /** When each learner of these courses was granted access, within a window (bucketed by day by the caller). */
    List<GrantRow> grantsBetween(Collection<String> courseIds, LocalDateTime start, LocalDateTime end);

    record CourseCount(String courseId, long total) {
    }

    record GrantRow(String courseId, LocalDateTime grantedAt) {
    }
}
