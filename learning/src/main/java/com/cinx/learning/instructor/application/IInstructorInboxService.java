package com.cinx.learning.instructor.application;

import com.cinx.learning.shared.domain.InboxKind;
import com.cinx.learning.instructor.application.query.InboxView;

public interface IInstructorInboxService {
    InboxView inbox(String instructorId, String courseId, InboxKind kind, String query);

    InboxView.Summary summary(String instructorId);
}
