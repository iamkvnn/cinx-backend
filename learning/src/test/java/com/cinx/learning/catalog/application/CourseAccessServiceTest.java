package com.cinx.learning.catalog.application;

import com.cinx.learning.catalog.application.query.CourseInfo;
import com.cinx.learning.catalog.domain.Course;
import com.cinx.learning.catalog.domain.CourseAccessPolicyRepository;
import com.cinx.learning.catalog.domain.CourseInstructorMembership;
import com.cinx.learning.catalog.domain.CourseInstructorMembershipRepository;
import com.cinx.learning.catalog.domain.CourseRepository;
import com.cinx.learning.entitlement.application.SubscriptionFeatureAccess;
import com.cinx.learning.enrollment.application.EnrollmentAccess;
import com.cinx.learning.shared.domain.CourseMembershipRole;
import com.cinx.learning.shared.domain.CourseMembershipStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseAccessServiceTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-07T03:00:00Z"), ZoneOffset.UTC);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 7, 3, 0);

    @Mock
    private EnrollmentAccess enrollmentAccess;
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private CourseInstructorMembershipRepository membershipRepository;
    @Mock
    private CourseAccessPolicyRepository accessPolicyRepository;
    @Mock
    private SubscriptionFeatureAccess subscriptionFeatureAccess;

    private CourseAccessService service;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        service = new CourseAccessService(enrollmentAccess, courseRepository, membershipRepository, accessPolicyRepository,
                subscriptionFeatureAccess, CLOCK);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void anActiveEditorMayEditButNotSubmit() {
        member("editor-1", CourseMembershipRole.EDITOR, CourseMembershipStatus.ACTIVE);

        assertThat(service.hasCourseCapability("editor-1", course(), CourseMembershipRole.EDITOR)).isTrue();
        assertThat(service.hasCourseCapability("editor-1", course(), CourseMembershipRole.MANAGER)).isFalse();
    }

    @Test
    void anActiveManagerMaySubmitButIsNotTheOwner() {
        member("manager-1", CourseMembershipRole.MANAGER, CourseMembershipStatus.ACTIVE);

        assertThat(service.hasCourseCapability("manager-1", course(), CourseMembershipRole.MANAGER)).isTrue();
        assertThat(service.hasCourseCapability("manager-1", course(), CourseMembershipRole.OWNER)).isFalse();
    }

    @Test
    void aPendingInvitationGrantsNothingEvenToTheListedOwner() {
        member("owner-1", CourseMembershipRole.OWNER, CourseMembershipStatus.PENDING);

        // a membership row decides: the primary-instructor fallback is only for owners without one
        assertThat(service.hasCourseCapability("owner-1", course(), CourseMembershipRole.EDITOR)).isFalse();
    }

    @Test
    void theCourseOwnerWithoutAMembershipRowKeepsFullAccess() {
        when(membershipRepository.findByCourseIdAndInstructorId(any(), any())).thenReturn(Optional.empty());

        assertThat(service.hasCourseCapability("owner-1", course(), CourseMembershipRole.OWNER)).isTrue();
        assertThat(service.hasCourseCapability("stranger", course(), CourseMembershipRole.EDITOR)).isFalse();
    }

    @Test
    void aCourseAnotherFeatureAlreadyHoldsIsJudgedLikeTheDomainCourse() {
        when(membershipRepository.findByCourseIdAndInstructorId(any(), any())).thenReturn(Optional.empty());
        CourseInfo info = CourseInfo.of(course());

        assertThat(service.hasCourseCapability("owner-1", info, CourseMembershipRole.OWNER)).isTrue();
        assertThat(service.hasCourseCapability("stranger", info, CourseMembershipRole.EDITOR)).isFalse();
    }

    @Test
    void anAnonymousCallerHasNoCapabilityAndNothingIsRead() {
        assertThat(service.hasCourseCapability(null, course(), CourseMembershipRole.EDITOR)).isFalse();
        assertThat(service.hasCourseCapability("owner-1", (Course) null, CourseMembershipRole.EDITOR)).isFalse();

        verifyNoInteractions(membershipRepository);
    }

    @Test
    void theUpdateAnyPermissionOpensEveryCourse() {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("admin-1", "n/a", "PERM_learning.course.update.any"));

        assertThat(service.hasCourseCapability("admin-1", course(), CourseMembershipRole.OWNER)).isTrue();
        verifyNoInteractions(membershipRepository);
    }

    @Test
    void enrolledMeansAnActiveEnrollmentAnActiveGrantOrALegacyEntitlement() {
        when(enrollmentAccess.hasActiveEnrollment("user-1", "course-1")).thenReturn(false);
        when(enrollmentAccess.hasActiveGrant("user-1", "course-1", NOW)).thenReturn(false);
        when(enrollmentAccess.isEntitled("user-1", "course-1")).thenReturn(true);

        assertThat(service.isEnrolled("user-1", "course-1")).isTrue();
        assertThat(service.isEnrolled(null, "course-1")).isFalse();
    }

    @Test
    void aLearnerWithNothingIsNotEnrolled() {
        when(enrollmentAccess.hasActiveEnrollment("user-1", "course-1")).thenReturn(false);
        when(enrollmentAccess.hasActiveGrant("user-1", "course-1", NOW)).thenReturn(false);
        when(enrollmentAccess.isEntitled("user-1", "course-1")).thenReturn(false);

        assertThat(service.isEnrolled("user-1", "course-1")).isFalse();
    }

    private void member(String instructorId, CourseMembershipRole role, CourseMembershipStatus status) {
        when(membershipRepository.findByCourseIdAndInstructorId("course-1", instructorId)).thenReturn(Optional.of(
                CourseInstructorMembership.builder().courseId("course-1").instructorId(instructorId).role(role).status(status).build()));
    }

    private Course course() {
        return Course.builder().id("course-1").instructorId("owner-1").build();
    }
}
