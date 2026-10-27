package com.cinx.learning.notificationprojection.application;

import java.util.List;

public interface INotificationProjectionService {
    List<String> getEnrolledUserIds(String courseId);
}
