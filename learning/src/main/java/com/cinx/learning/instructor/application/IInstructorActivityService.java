package com.cinx.learning.instructor.application;

import com.cinx.learning.shared.domain.ActivityAction;
import com.cinx.learning.shared.domain.InboxKind;
import com.cinx.learning.instructor.application.query.ActivityView;

import java.util.List;

public interface IInstructorActivityService {
    /** Records one action in the caller's transaction; the "Đã xử lý" log reads these rows. */
    void record(String instructorId, String courseId, InboxKind kind, ActivityAction action,
                String targetId, int itemCount, String summary);

    List<ActivityView> recent(String instructorId, InboxKind kind, String courseId, int days);
}
