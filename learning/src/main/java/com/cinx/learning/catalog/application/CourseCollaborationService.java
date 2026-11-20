package com.cinx.learning.catalog.application;

import com.cinx.learning.shared.application.UpstreamCallException;

import com.cinx.common.dto.ApiResponse;
import com.cinx.common.exception.AlreadyExistException;
import com.cinx.common.exception.BadRequestException;
import com.cinx.common.exception.ConflictException;
import com.cinx.common.exception.ErrorCode;
import com.cinx.common.exception.ForbiddenException;
import com.cinx.common.exception.NotFoundException;
import com.cinx.learning.shared.domain.CourseMembershipRole;
import com.cinx.learning.shared.domain.CourseMembershipStatus;
import com.cinx.learning.catalog.application.command.InviteCourseInstructorCommand;
import com.cinx.learning.catalog.application.command.UpdateCourseInstructorCommand;
import com.cinx.learning.catalog.application.query.CourseInstructorMembershipView;
import com.cinx.learning.catalog.application.query.InstructorLookupView;
import com.cinx.learning.catalog.application.query.InstructorLookupViewX;
import com.cinx.learning.catalog.application.query.InvitationView;
import com.cinx.learning.catalog.application.query.UserView;
import com.cinx.learning.catalog.application.CourseEvents;
import com.cinx.learning.catalog.application.event.CourseCollaboratorInvitedEvent;
import com.cinx.learning.catalog.domain.Course;
import com.cinx.learning.catalog.domain.CourseInstructorMembership;
import com.cinx.learning.catalog.domain.CourseInstructorMembershipRepository;
import com.cinx.learning.catalog.domain.CourseRepository;
import com.cinx.learning.catalog.application.InstructorDirectory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseCollaborationService implements ICourseCollaborationService {
    private final CourseRepository courseRepository;
    private final CourseInstructorMembershipRepository membershipRepository;
    private final InstructorDirectory userService;
    private final ICourseAccessService accessService;
    private final CourseEvents courseEventProducer;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public List<CourseInstructorMembershipView> list(String currentUserId, String courseId) {
        accessService.ensureManageableCourse(currentUserId, courseId);
        Course course = course(courseId);
        List<CourseInstructorMembership> memberships = membershipRepository.findByCourseIdOrderByCreatedAtAsc(courseId);
        Map<String, UserView> people = people(memberships.stream().map(CourseInstructorMembership::getInstructorId).toList());
        return memberships.stream()
                .map(membership -> toResponse(membership, course, people.get(membership.getInstructorId())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public InstructorLookupViewX lookup(String currentUserId, String courseId, String email) {
        accessService.ensureCanManageTeam(currentUserId, course(courseId));
        try {
            ApiResponse<InstructorLookupView> response = userService.lookupInstructor(email);
            InstructorLookupView found = response == null ? null : response.data();
            if (found == null) {
                throw notFoundInstructor();
            }
            return new InstructorLookupViewX(found.id(), found.name(), found.avatarUrl());
        } catch (UpstreamCallException e) {
            if (!e.isNotFound()) {
                throw e;
            }
            throw notFoundInstructor();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvitationView> myInvitations(String currentUserId) {
        List<CourseInstructorMembership> pending = membershipRepository
                .findByInstructorIdAndStatus(currentUserId, CourseMembershipStatus.PENDING);
        if (pending.isEmpty()) {
            return List.of();
        }
        Map<String, Course> courses = courseRepository.findAllById(pending.stream().map(CourseInstructorMembership::getCourseId).toList())
                .stream().collect(Collectors.toMap(Course::getId, Function.identity()));
        Map<String, UserView> inviters = people(pending.stream().map(CourseInstructorMembership::getInvitedBy).toList());
        return pending.stream()
                .filter(membership -> courses.containsKey(membership.getCourseId()))
                .sorted(Comparator.comparing(CourseInstructorMembership::getInvitedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(membership -> {
                    Course course = courses.get(membership.getCourseId());
                    UserView inviter = inviters.get(membership.getInvitedBy());
                    return new InvitationView(course.getId(), course.getTitle(), course.thumbnailUrl(),
                            membership.getRole(), membership.getInvitedAt(),
                            new InvitationView.Inviter(membership.getInvitedBy(),
                                    inviter == null ? null : inviter.name(), inviter == null ? null : inviter.avatarUrl()));
                })
                .toList();
    }

    private static NotFoundException notFoundInstructor() {
        return new NotFoundException(ErrorCode.INSTRUCTOR_NOT_FOUND, "No verified instructor uses this email");
    }

    /** Names and avatars are decoration: a user-service failure leaves them empty instead of failing the list. */
    private Map<String, UserView> people(List<String> ids) {
        List<String> distinct = ids.stream().filter(Objects::nonNull).distinct().toList();
        if (distinct.isEmpty()) {
            return Map.of();
        }
        try {
            ApiResponse<List<UserView>> response = userService.getInstructorsByIds(distinct);
            if (response == null || response.data() == null) {
                return Map.of();
            }
            return response.data().stream().collect(Collectors.toMap(UserView::userId, Function.identity(), (a, b) -> a));
        } catch (UpstreamCallException e) {
            return Map.of();
        }
    }

    @Override
    @Transactional
    public CourseInstructorMembershipView invite(String currentUserId, String courseId,
                                                      InviteCourseInstructorCommand request) {
        Course course = course(courseId);
        accessService.ensureCanManageTeam(currentUserId, course);
        if (request.role() == CourseMembershipRole.OWNER) {
            throw new BadRequestException(ErrorCode.BAD_REQUEST, "A new invitation cannot assign OWNER role");
        }
        if (Objects.equals(course.getInstructorId(), request.instructorId())) {
            throw new ConflictException(ErrorCode.RESOURCE_ALREADY_EXISTS,
                    "The primary instructor is already an owner of this course");
        }
        ensureVerifiedInstructor(request.instructorId());

        LocalDateTime now = now();
        CourseInstructorMembership membership = membershipRepository
                .findByCourseIdAndInstructorId(courseId, request.instructorId())
                .orElseGet(() -> CourseInstructorMembership.builder()
                        .courseId(courseId)
                        .instructorId(request.instructorId())
                        .build());

        if (membership.getStatus() == CourseMembershipStatus.ACTIVE) {
            throw new AlreadyExistException("Instructor is already an active course collaborator");
        }
        membership.setRole(request.role());
        membership.setStatus(CourseMembershipStatus.PENDING);
        membership.setInvitedBy(currentUserId);
        membership.setInvitedAt(now);
        membership.setAcceptedAt(null);
        membership.setRevokedAt(null);
        CourseInstructorMembership saved = membershipRepository.save(membership);
        // same transaction as the PENDING row (outbox); notification tells the invitee
        courseEventProducer.publishCollaboratorInvitedEvent(new CourseCollaboratorInvitedEvent(
                courseId, course.getTitle(), currentUserId, request.instructorId(), request.role(), now));
        return toResponse(saved, course);
    }

    @Override
    @Transactional
    public CourseInstructorMembershipView accept(String currentUserId, String courseId) {
        Course course = course(courseId);
        CourseInstructorMembership membership = membership(courseId, currentUserId);
        if (membership.getStatus() != CourseMembershipStatus.PENDING) {
            throw new ConflictException(ErrorCode.COURSE_STATUS_INVALID,
                    "There is no pending invitation for this instructor");
        }
        ensureVerifiedInstructor(currentUserId);
        membership.setStatus(CourseMembershipStatus.ACTIVE);
        membership.setAcceptedAt(now());
        membership.setRevokedAt(null);
        return toResponse(membershipRepository.save(membership), course);
    }

    @Override
    @Transactional
    public CourseInstructorMembershipView decline(String currentUserId, String courseId) {
        Course course = course(courseId);
        CourseInstructorMembership membership = membership(courseId, currentUserId);
        if (membership.getStatus() != CourseMembershipStatus.PENDING) {
            throw new ConflictException(ErrorCode.COURSE_STATUS_INVALID,
                    "There is no pending invitation for this instructor");
        }
        membership.setStatus(CourseMembershipStatus.DECLINED);
        return toResponse(membershipRepository.save(membership), course);
    }

    @Override
    @Transactional
    public CourseInstructorMembershipView updateRole(String currentUserId, String courseId, String instructorId,
                                                          UpdateCourseInstructorCommand request) {
        Course course = course(courseId);
        accessService.ensureCanManageTeam(currentUserId, course);
        if (request.role() == CourseMembershipRole.OWNER) {
            throw new BadRequestException(ErrorCode.BAD_REQUEST,
                    "Use ownership transfer to assign OWNER role");
        }
        CourseInstructorMembership membership = membership(courseId, instructorId);
        if (membership.getRole() == CourseMembershipRole.OWNER) {
            throw new ForbiddenException(ErrorCode.NOT_RESOURCE_OWNER,
                    "The course owner role cannot be changed directly");
        }
        if (membership.getStatus() != CourseMembershipStatus.ACTIVE
                && membership.getStatus() != CourseMembershipStatus.PENDING) {
            throw new ConflictException(ErrorCode.COURSE_STATUS_INVALID,
                    "Only pending or active collaborators can change role");
        }
        membership.setRole(request.role());
        return toResponse(membershipRepository.save(membership), course);
    }

    @Override
    @Transactional
    public void revoke(String currentUserId, String courseId, String instructorId) {
        Course course = course(courseId);
        accessService.ensureCanManageTeam(currentUserId, course);
        CourseInstructorMembership membership = membership(courseId, instructorId);
        if (membership.getRole() == CourseMembershipRole.OWNER) {
            throw new ForbiddenException(ErrorCode.NOT_RESOURCE_OWNER,
                    "The course owner cannot be revoked");
        }
        membership.setStatus(CourseMembershipStatus.REVOKED);
        membership.setRevokedAt(now());
        membershipRepository.save(membership);
    }

    @Override
    @Transactional
    public void transferOwnership(String currentUserId, String courseId, String instructorId) {
        Course course = course(courseId);
        accessService.ensureCanManageTeam(currentUserId, course);
        if (Objects.equals(currentUserId, instructorId)) {
            return;
        }
        CourseInstructorMembership currentOwner = membershipRepository
                .findByCourseIdAndInstructorId(courseId, course.getInstructorId())
                .orElseThrow(() -> new NotFoundException("Course owner membership not found"));
        CourseInstructorMembership nextOwner = membership(courseId, instructorId);
        if (nextOwner.getStatus() != CourseMembershipStatus.ACTIVE) {
            throw new ConflictException(ErrorCode.COURSE_STATUS_INVALID,
                    "Ownership can only be transferred to an active collaborator");
        }
        currentOwner.setRole(CourseMembershipRole.MANAGER);
        nextOwner.setRole(CourseMembershipRole.OWNER);
        course.setInstructorId(instructorId);
        courseRepository.save(course);
        membershipRepository.save(currentOwner);
        membershipRepository.save(nextOwner);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> activeInstructorIds(String courseId) {
        return membershipRepository.findByCourseIdAndStatusOrderByCreatedAtAsc(
                        courseId, CourseMembershipStatus.ACTIVE)
                .stream()
                .map(CourseInstructorMembership::getInstructorId)
                .toList();
    }

    @Override
    @Transactional
    public void initializeOwner(String courseId, String instructorId) {
        if (membershipRepository.findByCourseIdAndInstructorId(courseId, instructorId).isPresent()) {
            return;
        }
        LocalDateTime now = now();
        membershipRepository.save(CourseInstructorMembership.builder()
                .courseId(courseId)
                .instructorId(instructorId)
                .role(CourseMembershipRole.OWNER)
                .status(CourseMembershipStatus.ACTIVE)
                .invitedBy(instructorId)
                .invitedAt(now)
                .acceptedAt(now)
                .build());
    }

    private void ensureVerifiedInstructor(String instructorId) {
        ApiResponse<Boolean> response = userService.isInstructorVerified(instructorId);
        if (response == null || !response.success() || !Boolean.TRUE.equals(response.data())) {
            throw new ForbiddenException(ErrorCode.INSTRUCTOR_NOT_VERIFIED,
                    "An active verified instructor partnership is required");
        }
    }

    private Course course(String courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException("Course not found with id: " + courseId));
    }

    private CourseInstructorMembership membership(String courseId, String instructorId) {
        return membershipRepository.findByCourseIdAndInstructorId(courseId, instructorId)
                .orElseThrow(() -> new NotFoundException("Course instructor membership not found"));
    }

    private CourseInstructorMembershipView toResponse(CourseInstructorMembership membership, Course course) {
        return toResponse(membership, course, people(List.of(membership.getInstructorId())).get(membership.getInstructorId()));
    }

    private CourseInstructorMembershipView toResponse(CourseInstructorMembership membership, Course course, UserView person) {
        return new CourseInstructorMembershipView(
                membership.getInstructorId(),
                membership.getRole(),
                membership.getStatus(),
                membership.getInvitedBy(),
                membership.getInvitedAt(),
                membership.getAcceptedAt(),
                membership.getRevokedAt(),
                Objects.equals(membership.getInstructorId(), course.getInstructorId()),
                person == null ? null : person.name(),
                person == null ? null : person.avatarUrl()
        );
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
