package com.cinx.learning.enrollment.application;

import com.cinx.common.dto.ApiResponse;
import com.cinx.learning.enrollment.application.query.CheckEnrollmentStatus;
import com.cinx.learning.enrollment.application.query.CourseEntitlementInfo;
import com.cinx.learning.enrollment.domain.CourseAccessGrantRepository;
import com.cinx.learning.enrollment.domain.CourseEnrollmentRepository;
import com.cinx.learning.enrollment.domain.CourseEntitlement;
import com.cinx.learning.enrollment.domain.CourseEntitlementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EnrollmentAccessService implements EnrollmentAccess {
    private static final String ACTIVE = "ACTIVE";

    private final CourseEntitlementRepository entitlementRepository;
    private final CourseAccessGrantRepository accessGrantRepository;
    private final CourseEnrollmentRepository enrollmentRepository;

    @Override
    public boolean hasActiveGrant(String userId, String courseId, LocalDateTime now) {
        return accessGrantRepository.existsActive(userId, courseId, ACTIVE, now);
    }

    @Override
    public boolean isEntitled(String userId, String courseId) {
        return entitlementRepository.existsByUserIdAndCourseId(userId, courseId);
    }

    @Override
    public boolean hasActiveEnrollment(String userId, String courseId) {
        return enrollmentRepository.existsByUserIdAndCourseIdAndStatus(userId, courseId, ACTIVE);
    }

    @Override
    public boolean courseHasEntitlements(String courseId) {
        return entitlementRepository.existsByCourseId(courseId);
    }

    @Override
    public Set<String> entitledCourseIds(String userId, Collection<String> courseIds) {
        return entitlementRepository.findAllByUserIdAndCourseIdIn(userId, courseIds).stream()
                .map(CourseEntitlement::getCourseId)
                .collect(Collectors.toSet());
    }

    @Override
    public ApiResponse<List<CheckEnrollmentStatus>> checkEnrollmentStatus(String userId, List<String> courseIds) {
        Set<String> entitled = entitledCourseIds(userId, courseIds);
        return ApiResponse.success("Enrollment status fetched", courseIds.stream()
                .map(courseId -> new CheckEnrollmentStatus(courseId, entitled.contains(courseId))).toList());
    }

    @Override
    public List<String> userIdsOfCourse(String courseId) {
        return entitlementRepository.findUserIdsByCourseId(courseId);
    }

    @Override
    public List<CourseEntitlementInfo> entitlementsOfCourse(String courseId) {
        return entitlementRepository.findAllByCourseId(courseId).stream().map(CourseEntitlementInfo::of).toList();
    }

    @Override
    public long countDistinctLearners() {
        return enrollmentRepository.countDistinctLearners();
    }

    @Override
    public List<CourseCount> countPerCourse(Collection<String> courseIds) {
        return entitlementRepository.countPerCourse(courseIds).stream()
                .map(row -> new CourseCount(row.getCourseId(), row.getTotal()))
                .toList();
    }

    @Override
    public List<GrantRow> grantsBetween(Collection<String> courseIds, LocalDateTime start, LocalDateTime end) {
        return entitlementRepository.findGrantsBetween(courseIds, start, end).stream()
                .map(row -> new GrantRow(row.getCourseId(), row.getGrantedAt()))
                .toList();
    }
}
