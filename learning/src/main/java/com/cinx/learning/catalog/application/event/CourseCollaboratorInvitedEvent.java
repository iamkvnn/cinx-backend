package com.cinx.learning.catalog.application.event;

import com.cinx.learning.shared.domain.CourseMembershipRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** v1. Ids and the course title only; notification looks names up itself (operator: "Chỉ id + tên khoá"). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourseCollaboratorInvitedEvent {
    private String courseId;
    private String courseTitle;
    private String inviterId;
    private String inviteeId;
    private CourseMembershipRole role;
    private LocalDateTime invitedAt;
}
