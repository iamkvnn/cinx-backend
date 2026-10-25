package com.cinx.learning.catalog.application;

import com.cinx.learning.catalog.application.query.CourseInfo;
import com.cinx.learning.catalog.domain.Course;
import com.cinx.learning.shared.domain.CourseMembershipRole;

import java.util.List;
import java.util.Map;

public interface ICourseAccessService {
    Course ensureReadableCourse(String currentUserId, String courseId);
    /** The public course page: a published course's intro is open to everyone; otherwise the read rules apply. */
    Course ensureViewableCourse(String currentUserId, String courseId);
    Course ensureManageableCourse(String currentUserId, String courseId);

    /** A course a reviewer reads; the caller's review permission is checked by the endpoint, not here. */
    Course reviewableCourse(String courseId);
    boolean canReadCourse(String currentUserId, Course course);
    boolean canReadCourse(String currentUserId, Course course, Map<String, Boolean> enrollmentByCourseId);
    boolean canManageCourse(String currentUserId, Course course);
    /** False when the course does not exist. */
    boolean canManageCourse(String currentUserId, String courseId);
    boolean isActiveMember(String currentUserId, Course course);
    boolean isActiveMember(String currentUserId, String courseId);
    boolean hasCourseCapability(String currentUserId, Course course, CourseMembershipRole requiredRole);
    boolean hasCourseCapability(String currentUserId, String courseId, CourseMembershipRole requiredRole);
    /** The same check for a course another feature already holds, without reading it again. */
    boolean hasCourseCapability(String currentUserId, CourseInfo course, CourseMembershipRole requiredRole);
    Map<String, Boolean> enrollmentByCourseId(String currentUserId, List<Course> courses);
    boolean isAdmin();
    boolean isCourseOwner(String currentUserId, Course course);
    boolean isEnrolled(String currentUserId, String courseId);

    /** True when the current local subscription projection grants this course. */
    default boolean hasSubscriptionAccess(String currentUserId, String courseId) {
        return false;
    }
    void ensureCurrentUserOwns(String currentUserId, Course course);
    void ensureEditableCourse(String currentUserId, Course course);
    void ensureCanSubmitCourse(String currentUserId, Course course);
    void ensureCanArchiveCourse(String currentUserId, Course course);
    void ensureCanManageTeam(String currentUserId, Course course);
}

