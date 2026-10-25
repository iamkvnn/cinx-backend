package com.cinx.learning.catalog.application;

import com.cinx.learning.catalog.application.command.InviteCourseInstructorCommand;
import com.cinx.learning.catalog.application.command.UpdateCourseInstructorCommand;
import com.cinx.learning.catalog.application.query.CourseInstructorMembershipView;
import com.cinx.learning.catalog.application.query.InstructorLookupViewX;
import com.cinx.learning.catalog.application.query.InvitationView;

import java.util.List;

public interface ICourseCollaborationService {
    List<CourseInstructorMembershipView> list(String currentUserId, String courseId);

    CourseInstructorMembershipView invite(String currentUserId, String courseId, InviteCourseInstructorCommand request);

    CourseInstructorMembershipView accept(String currentUserId, String courseId);

    CourseInstructorMembershipView decline(String currentUserId, String courseId);

    CourseInstructorMembershipView updateRole(String currentUserId, String courseId, String instructorId,
                                                   UpdateCourseInstructorCommand request);

    void revoke(String currentUserId, String courseId, String instructorId);

    void transferOwnership(String currentUserId, String courseId, String instructorId);

    List<String> activeInstructorIds(String courseId);

    /** The owner looks up the one verified instructor behind an exact email before inviting them. */
    InstructorLookupViewX lookup(String currentUserId, String courseId, String email);

    /** The caller's pending invitations, newest first. */
    List<InvitationView> myInvitations(String currentUserId);

    void initializeOwner(String courseId, String instructorId);
}
