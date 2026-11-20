package com.cinx.learning.notificationprojection.application;

import com.cinx.learning.enrollment.application.EnrollmentAccess;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationProjectionService implements INotificationProjectionService {
    private final EnrollmentAccess enrollmentAccess;

    @Override
    @Transactional(readOnly = true)
    public List<String> getEnrolledUserIds(String courseId) {
        return enrollmentAccess.userIdsOfCourse(courseId);
    }
}
