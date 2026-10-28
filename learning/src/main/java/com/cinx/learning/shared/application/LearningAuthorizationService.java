package com.cinx.learning.shared.application;

import com.cinx.common.exception.ErrorCode;
import com.cinx.common.exception.ForbiddenException;
import com.cinx.common.utils.AuthenticationUtil;
import com.cinx.learning.courseview.application.CourseService;
import com.cinx.learning.catalog.application.ICourseAccessService;
import com.cinx.learning.shared.domain.CourseMembershipRole;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class LearningAuthorizationService {
    private final CourseService courseService;
    private final ICourseAccessService courseAccessService;

    public boolean isAdmin() {
        return hasAuthority("PERM_learning.analytics.read.any")
                || hasAuthority("PERM_learning.course.review.any")
                || hasAuthority("PERM_learning.course.update.any");
    }

    public boolean isInstructor() {
        return hasAuthority("PERM_learning.analytics.read.own")
                || hasAuthority("PERM_learning.submission.grade.own")
                || hasAuthority("PERM_learning.course.update.own");
    }

    public void requireCourseInstructor(String currentUserId, String courseId) {
        if (hasAuthority("PERM_learning.analytics.read.any")) {
            return;
        }
        if (isCourseInstructor(courseId, currentUserId)) {
            return;
        }
        throw new ForbiddenException(ErrorCode.INSTRUCTOR_ACCESS_REQUIRED, "Only the course instructor can access this resource");
    }

    public void requireLessonInstructor(String currentUserId, String lessonId) {
        if (isLessonInstructor(lessonId, currentUserId)) {
            return;
        }
        throw new ForbiddenException(ErrorCode.INSTRUCTOR_ACCESS_REQUIRED, "Only the lesson instructor can access this resource");
    }

    public boolean isCourseInstructor(String courseId, String userId) {
        if (userId == null) {
            return false;
        }
        return courseAccessService.hasCourseCapability(userId, courseId, CourseMembershipRole.MANAGER);
    }

    /** Who may read and write a course's Q&A: whoever may study it, as LessonService checks before a lesson. */
    public boolean canParticipateInCourse(String userId, String courseId) {
        if (userId == null || courseId == null) {
            return false;
        }
        return courseAccessService.isAdmin()
                || courseAccessService.isActiveMember(userId, courseId)
                || courseAccessService.isEnrolled(userId, courseId)
                || courseAccessService.hasSubscriptionAccess(userId, courseId);
    }

    public boolean isLessonInstructor(String lessonId, String userId) {
        if (userId == null) {
            return false;
        }
        var response = courseService.isLessonInstructor(lessonId, userId);
        return response != null && Boolean.TRUE.equals(response.data());
    }

    private boolean hasAuthority(String expected) {
        Authentication authentication = AuthenticationUtil.getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(expected::equals);
    }
}
