package com.cinx.notification.notification.infrastructure.messaging.event.course;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** learning `course.collaborator.invited` v1: ids and the course title; names are looked up here. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourseCollaboratorInvitedEvent {
    private String courseId;
    private String courseTitle;
    private String inviterId;
    private String inviteeId;
    private String role;
    private String invitedAt;
}
