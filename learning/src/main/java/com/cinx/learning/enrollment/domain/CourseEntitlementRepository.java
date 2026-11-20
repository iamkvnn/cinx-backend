package com.cinx.learning.enrollment.domain;

import java.util.Collection;
import java.util.List;

/** Port: where CourseEntitlement rows are stored. */
public interface CourseEntitlementRepository {
    boolean existsByUserIdAndCourseId(String userId, String courseId);

    boolean existsByCourseId(String courseId);

    List<CourseEntitlement> findAllByCourseId(String courseId);

    List<CourseEntitlement> findAllByUserIdAndCourseIdIn(String userId, Collection<String> courseIds);

    List<CourseEntitlement> findAllByUserIdOrderByGrantedAtDesc(String userId);

    List<String> findUserIdsByCourseId(String courseId);

    List<CourseCount> countPerCourse(Collection<String> courseIds);

    /** When each learner of these courses was granted access, within a window (bucketed by day by the caller). */
    List<GrantRow> findGrantsBetween(Collection<String> courseIds, java.time.LocalDateTime start, java.time.LocalDateTime end);

    boolean existsById(String id);

    CourseEntitlement saveAndFlush(CourseEntitlement entity);

    interface CourseCount {
            String getCourseId();
            long getTotal();
        }

    interface GrantRow {
            String getCourseId();
            java.time.LocalDateTime getGrantedAt();
        }
}
