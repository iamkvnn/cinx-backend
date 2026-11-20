package com.cinx.learning.catalog.application;

import com.cinx.common.dto.ApiResponse;
import com.cinx.common.exception.ForbiddenException;
import com.cinx.learning.shared.domain.CourseMembershipRole;
import com.cinx.learning.shared.domain.CourseMembershipStatus;
import com.cinx.learning.catalog.application.command.InviteCourseInstructorCommand;
import com.cinx.learning.catalog.domain.Course;
import com.cinx.learning.catalog.domain.CourseInstructorMembership;
import com.cinx.learning.catalog.domain.CourseInstructorMembershipRepository;
import com.cinx.learning.catalog.domain.CourseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseCollaborationServiceTest {
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private CourseInstructorMembershipRepository membershipRepository;
    @Mock
    private InstructorDirectory userService;
    @Mock
    private ICourseAccessService accessService;
    @Mock
    private CourseEvents courseEventProducer;

    @Mock
    private Clock clock;
    @InjectMocks
    private CourseCollaborationService service;

    @BeforeEach
    void setUpClock() {
        lenient().when(clock.instant()).thenReturn(Instant.parse("2026-01-01T00:00:00Z"));
        lenient().when(clock.getZone()).thenReturn(ZoneOffset.UTC);
    }

    @Test
    void inviteRequiresVerifiedInstructorAndCreatesPendingMembership() {
        Course course = course("course-1", "owner-1");
        when(courseRepository.findById("course-1")).thenReturn(Optional.of(course));
        when(userService.isInstructorVerified("instructor-2"))
                .thenReturn(new ApiResponse<>(true, "ok", true));
        when(membershipRepository.findByCourseIdAndInstructorId("course-1", "instructor-2"))
                .thenReturn(Optional.empty());
        when(membershipRepository.save(any(CourseInstructorMembership.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.invite("owner-1", "course-1",
                new InviteCourseInstructorCommand("instructor-2", CourseMembershipRole.EDITOR));

        assertThat(response.instructorId()).isEqualTo("instructor-2");
        assertThat(response.role()).isEqualTo(CourseMembershipRole.EDITOR);
        assertThat(response.status()).isEqualTo(CourseMembershipStatus.PENDING);
        verify(accessService).ensureCanManageTeam("owner-1", course);
    }

    @Test
    void acceptMovesPendingInvitationToActive() {
        Course course = course("course-1", "owner-1");
        CourseInstructorMembership membership = membership("course-1", "instructor-2",
                CourseMembershipRole.MANAGER, CourseMembershipStatus.PENDING);
        when(courseRepository.findById("course-1")).thenReturn(Optional.of(course));
        when(membershipRepository.findByCourseIdAndInstructorId("course-1", "instructor-2"))
                .thenReturn(Optional.of(membership));
        when(userService.isInstructorVerified("instructor-2"))
                .thenReturn(new ApiResponse<>(true, "ok", true));
        when(membershipRepository.save(any(CourseInstructorMembership.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.accept("instructor-2", "course-1");

        assertThat(response.status()).isEqualTo(CourseMembershipStatus.ACTIVE);
        assertThat(membership.getAcceptedAt()).isNotNull();
    }

    @Test
    void ownerCannotBeRevoked() {
        Course course = course("course-1", "owner-1");
        CourseInstructorMembership owner = membership("course-1", "owner-1",
                CourseMembershipRole.OWNER, CourseMembershipStatus.ACTIVE);
        when(courseRepository.findById("course-1")).thenReturn(Optional.of(course));
        when(membershipRepository.findByCourseIdAndInstructorId("course-1", "owner-1"))
                .thenReturn(Optional.of(owner));

        assertThatThrownBy(() -> service.revoke("owner-1", "course-1", "owner-1"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void transferChangesPrimaryInstructorAndMembershipRoles() {
        Course course = course("course-1", "owner-1");
        CourseInstructorMembership owner = membership("course-1", "owner-1",
                CourseMembershipRole.OWNER, CourseMembershipStatus.ACTIVE);
        CourseInstructorMembership nextOwner = membership("course-1", "instructor-2",
                CourseMembershipRole.MANAGER, CourseMembershipStatus.ACTIVE);
        when(courseRepository.findById("course-1")).thenReturn(Optional.of(course));
        when(membershipRepository.findByCourseIdAndInstructorId("course-1", "owner-1"))
                .thenReturn(Optional.of(owner));
        when(membershipRepository.findByCourseIdAndInstructorId("course-1", "instructor-2"))
                .thenReturn(Optional.of(nextOwner));

        service.transferOwnership("owner-1", "course-1", "instructor-2");

        assertThat(course.getInstructorId()).isEqualTo("instructor-2");
        assertThat(owner.getRole()).isEqualTo(CourseMembershipRole.MANAGER);
        assertThat(nextOwner.getRole()).isEqualTo(CourseMembershipRole.OWNER);
        verify(courseRepository).save(course);
    }

    private Course course(String id, String instructorId) {
        return Course.builder().id(id).instructorId(instructorId).build();
    }

    private CourseInstructorMembership membership(String courseId, String instructorId,
                                                   CourseMembershipRole role, CourseMembershipStatus status) {
        return CourseInstructorMembership.builder()
                .courseId(courseId)
                .instructorId(instructorId)
                .role(role)
                .status(status)
                .build();
    }
}
