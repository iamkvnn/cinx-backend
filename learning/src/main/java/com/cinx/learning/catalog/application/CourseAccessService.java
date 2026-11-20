package com.cinx.learning.catalog.application;

import com.cinx.learning.catalog.application.query.CourseInfo;

import com.cinx.learning.entitlement.application.SubscriptionFeatureAccess;

import com.cinx.learning.enrollment.application.EnrollmentAccess;

import com.cinx.common.exception.ErrorCode;
import com.cinx.common.exception.ForbiddenException;
import com.cinx.common.exception.NotFoundException;
import com.cinx.common.utils.AuthenticationUtil;
import com.cinx.learning.shared.domain.CourseStatus;
import com.cinx.learning.catalog.domain.CourseAccessModel;
import com.cinx.learning.shared.domain.CourseMembershipRole;
import com.cinx.learning.shared.domain.CourseMembershipStatus;
import com.cinx.learning.catalog.domain.CourseAccessPolicy;
import com.cinx.learning.catalog.domain.Course;
import com.cinx.learning.catalog.domain.CourseAccessPolicyRepository;
import com.cinx.learning.catalog.domain.CourseInstructorMembershipRepository;
import com.cinx.learning.catalog.domain.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CourseAccessService implements ICourseAccessService {
    private final EnrollmentAccess enrollmentAccess;
    private final CourseRepository courseRepository;
    private final CourseInstructorMembershipRepository membershipRepository;
    private final CourseAccessPolicyRepository accessPolicyRepository;
    private final SubscriptionFeatureAccess subscriptionFeatureAccess;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public Course ensureReadableCourse(String currentUserId, String courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException("Course not found with id: " + courseId));
        if (!canReadCourse(currentUserId, course)) {
            throw new NotFoundException("Course not found with id: " + courseId);
        }
        return course;
    }

    @Override
    @Transactional(readOnly = true)
    public Course ensureViewableCourse(String currentUserId, String courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException("Course not found with id: " + courseId));
        if (course.getStatus() != CourseStatus.PUBLISHED && !canReadCourse(currentUserId, course)) {
            throw new NotFoundException("Course not found with id: " + courseId);
        }
        return course;
    }

    @Override
    @Transactional(readOnly = true)
    public Course ensureManageableCourse(String currentUserId, String courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException("Course not found with id: " + courseId));
        if (!canManageCourse(currentUserId, course)) {
            throw new ForbiddenException(ErrorCode.NOT_RESOURCE_OWNER, "You are not allowed to access this course");
        }
        return course;
    }

    @Override
    @Transactional(readOnly = true)
    public Course reviewableCourse(String courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException("Course not found with id: " + courseId));
    }

    @Override
    public boolean canReadCourse(String currentUserId, Course course) {
        if (currentUserId != null && (isAdmin() || isActiveMember(currentUserId, course))) {
            return true;
        }
        CourseAccessPolicy policy = accessPolicyRepository.findById(course.getId()).orElse(null);
        if (policy == null) {
            // Compatibility for records created during the additive event
            // window. Paid courses fail closed until an admin policy or a
            // permanent grant is present; free courses remain public.
            if (course.getStatus() != CourseStatus.PUBLISHED) {
                return false;
            }
            if (course.getPrice() == null || course.getPrice().signum() <= 0) {
                return true;
            }
            if (currentUserId == null) {
                return false;
            }
            boolean permanent = enrollmentAccess.hasActiveGrant(currentUserId, course.getId(), now());
            boolean legacy = enrollmentAccess.isEntitled(currentUserId, course.getId());
            boolean subscription = Boolean.TRUE.equals(course.getIsInSubscription())
                    && hasSubscriptionFeature(currentUserId, "learning.subscription-catalog.access");
            return permanent || legacy || subscription;
        }
        if (course.getStatus() != CourseStatus.PUBLISHED && currentUserId == null) {
            return false;
        }
        LocalDateTime now = now();
        if (policy.getEffectiveFrom() != null && now.isBefore(policy.getEffectiveFrom())) {
            return false;
        }
        if (policy.getEffectiveUntil() != null && !now.isBefore(policy.getEffectiveUntil())) {
            return false;
        }
        if (currentUserId == null) {
            return false;
        }
        boolean permanent = enrollmentAccess.hasActiveGrant(currentUserId, course.getId(), now());
        boolean legacy = enrollmentAccess.isEntitled(currentUserId, course.getId());
        boolean subscription = policy.getAccessModel() != CourseAccessModel.PURCHASE_ONLY
                && hasSubscriptionFeature(currentUserId, policy.getSubscriptionFeatureKey());
        return permanent || legacy || subscription;
    }

    @Override
    public boolean canReadCourse(String currentUserId, Course course, Map<String, Boolean> enrollmentByCourseId) {
        if (currentUserId != null && (isAdmin() || isActiveMember(currentUserId, course))) {
            return true;
        }
        CourseAccessPolicy policy = accessPolicyRepository.findById(course.getId()).orElse(null);
        if (policy == null) {
            return canReadCourse(currentUserId, course);
        }
        // Enrollment is historical user state, not proof of current access.
        // A subscription-backed course must still pass the live local grant or
        // entitlement resolver after the subscription period expires.
        return currentUserId != null && canReadCourse(currentUserId, course);
    }

    @Override
    public boolean canManageCourse(String currentUserId, Course course) {
        return hasPermission("PERM_learning.course.update.any")
                || isActiveMember(currentUserId, course);
    }

    @Override
    public boolean canManageCourse(String currentUserId, String courseId) {
        return courseRepository.findById(courseId)
                .map(course -> canManageCourse(currentUserId, course))
                .orElse(false);
    }

    @Override
    public boolean isActiveMember(String currentUserId, Course course) {
        if (currentUserId == null || course == null) {
            return false;
        }
        var membership = membershipRepository.findByCourseIdAndInstructorId(course.getId(), currentUserId);
        if (membership.isPresent()) {
            return membership.get().getStatus() == CourseMembershipStatus.ACTIVE;
        }
        // Compatibility only for the short window before V10 has backfilled
        // legacy owner rows.
        return Objects.equals(course.getInstructorId(), currentUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isActiveMember(String currentUserId, String courseId) {
        if (currentUserId == null || courseId == null) {
            return false;
        }
        Course course = courseRepository.findById(courseId).orElse(null);
        return course != null && isActiveMember(currentUserId, course);
    }

    @Override
    public boolean hasCourseCapability(String currentUserId, Course course, CourseMembershipRole requiredRole) {
        return course != null && hasCourseCapability(currentUserId, course.getId(), course.getInstructorId(), requiredRole);
    }

    @Override
    public boolean hasCourseCapability(String currentUserId, CourseInfo course, CourseMembershipRole requiredRole) {
        return course != null && hasCourseCapability(currentUserId, course.getId(), course.getInstructorId(), requiredRole);
    }

    private boolean hasCourseCapability(String currentUserId, String courseId, String ownerId, CourseMembershipRole requiredRole) {
        if (currentUserId == null) {
            return false;
        }
        if (hasPermission("PERM_learning.course.update.any")) {
            return true;
        }
        var membership = membershipRepository.findByCourseIdAndInstructorId(courseId, currentUserId);
        if (membership.isPresent()) {
            return membership.get().getStatus() == CourseMembershipStatus.ACTIVE
                    && roleSatisfies(membership.get().getRole(), requiredRole);
        }
        // Compatibility only for the short window before V10 has backfilled
        // legacy owner rows.
        return Objects.equals(ownerId, currentUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasCourseCapability(String currentUserId, String courseId, CourseMembershipRole requiredRole) {
        if (currentUserId == null || courseId == null) {
            return false;
        }
        Course course = courseRepository.findById(courseId).orElse(null);
        return course != null && hasCourseCapability(currentUserId, course, requiredRole);
    }

    @Override
    public Map<String, Boolean> enrollmentByCourseId(String currentUserId, List<Course> courses) {
        if (currentUserId == null || isAdmin()) {
            return Map.of();
        }
        List<String> courseIds = courses.stream()
                .filter(course -> !isActiveMember(currentUserId, course))
                .map(Course::getId)
                .distinct()
                .toList();
        if (courseIds.isEmpty()) {
            return Map.of();
        }
        Map<String, Boolean> result = new java.util.HashMap<>();
        courseIds.forEach(courseId -> result.put(courseId, false));
        enrollmentAccess.entitledCourseIds(currentUserId, courseIds)
                .forEach(entitledCourseId -> result.put(entitledCourseId, true));
        courseIds.forEach(courseId -> {
            if (enrollmentAccess.hasActiveGrant(currentUserId, courseId, now())
                    || enrollmentAccess.hasActiveEnrollment(currentUserId, courseId)) {
                result.put(courseId, true);
            }
        });
        return result;
    }

    @Override
    public boolean isAdmin() {
        Authentication authentication = AuthenticationUtil.getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> authority.equals("PERM_learning.course.update.any")
                        || authority.equals("PERM_learning.course.review.any"));
    }

    @Override
    public boolean isCourseOwner(String currentUserId, Course course) {
        if (currentUserId == null || course == null || !Objects.equals(course.getInstructorId(), currentUserId)) {
            return false;
        }
        return membershipRepository.findByCourseIdAndInstructorId(course.getId(), currentUserId)
                .map(membership -> membership.getStatus() == CourseMembershipStatus.ACTIVE
                        && membership.getRole() == CourseMembershipRole.OWNER)
                .orElse(true);
    }

    @Override
    public boolean isEnrolled(String currentUserId, String courseId) {
        if (currentUserId == null) {
            return false;
        }
        return enrollmentAccess.hasActiveEnrollment(currentUserId, courseId)
                || enrollmentAccess.hasActiveGrant(currentUserId, courseId, now())
                || enrollmentAccess.isEntitled(currentUserId, courseId);
    }

    @Override
    public boolean hasSubscriptionAccess(String currentUserId, String courseId) {
        if (currentUserId == null || courseId == null) return false;
        Course course = courseRepository.findById(courseId).orElse(null);
        if (course == null) return false;
        CourseAccessPolicy policy = accessPolicyRepository.findById(courseId).orElse(null);
        if (policy == null || (policy.getAccessModel() != CourseAccessModel.SUBSCRIPTION_ONLY
                && policy.getAccessModel() != CourseAccessModel.PURCHASE_OR_SUBSCRIPTION)) {
            return false;
        }
        return hasSubscriptionFeature(currentUserId, policy.getSubscriptionFeatureKey());
    }

    @Override
    public void ensureCurrentUserOwns(String currentUserId, Course course) {
        if (!hasCourseCapability(currentUserId, course, CourseMembershipRole.EDITOR)) {
            throw new ForbiddenException(ErrorCode.NOT_RESOURCE_OWNER, "You are not allowed to access this course");
        }
    }

    @Override
    public void ensureEditableCourse(String currentUserId, Course course) {
        if (!hasCourseCapability(currentUserId, course, CourseMembershipRole.EDITOR)) {
            throw new ForbiddenException(ErrorCode.NOT_RESOURCE_OWNER, "You are not allowed to access this course");
        }
        if (course.getStatus() == CourseStatus.ARCHIVED) {
            throw new ForbiddenException(ErrorCode.COURSE_ARCHIVED,
                    "Archived courses cannot be edited");
        }
        if (course.getPublishStatus() == com.cinx.learning.shared.domain.CoursePublishStatus.WAITING_APPROVAL) {
            throw new ForbiddenException(ErrorCode.COURSE_WAITING_APPROVAL,
                    "Courses waiting for approval cannot be edited");
        }
    }

    @Override
    public void ensureCanSubmitCourse(String currentUserId, Course course) {
        if (!hasCourseCapability(currentUserId, course, CourseMembershipRole.MANAGER)) {
            throw new ForbiddenException(ErrorCode.INSTRUCTOR_ACCESS_REQUIRED,
                    "Only an owner or course manager can submit this course");
        }
        if (course.getStatus() == CourseStatus.ARCHIVED) {
            throw new ForbiddenException(ErrorCode.COURSE_ARCHIVED, "Archived courses cannot be submitted");
        }
    }

    @Override
    public void ensureCanArchiveCourse(String currentUserId, Course course) {
        if (!hasCourseCapability(currentUserId, course, CourseMembershipRole.OWNER)) {
            throw new ForbiddenException(ErrorCode.INSTRUCTOR_ACCESS_REQUIRED,
                    "Only the course owner can archive this course");
        }
    }

    @Override
    public void ensureCanManageTeam(String currentUserId, Course course) {
        if (!hasCourseCapability(currentUserId, course, CourseMembershipRole.OWNER)) {
            throw new ForbiddenException(ErrorCode.INSTRUCTOR_ACCESS_REQUIRED,
                    "Only the course owner can manage collaborators");
        }
    }

    private boolean roleSatisfies(CourseMembershipRole actual, CourseMembershipRole required) {
        return switch (required) {
            case EDITOR -> actual.canEdit();
            case MANAGER -> actual.canSubmit();
            case OWNER -> actual == CourseMembershipRole.OWNER;
        };
    }

    private boolean hasPermission(String expected) {
        Authentication authentication = AuthenticationUtil.getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(expected::equals);
    }

    private boolean hasSubscriptionFeature(String userId, String featureKey) {
        return subscriptionFeatureAccess.hasFeature(userId, featureKey, now());
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}

