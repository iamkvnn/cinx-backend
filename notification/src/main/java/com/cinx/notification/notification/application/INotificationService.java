package com.cinx.notification.notification.application;

import com.cinx.common.paging.PageResult;
import com.cinx.notification.notification.application.command.SendNotificationCommand;
import com.cinx.notification.notification.application.query.NotificationCounts;
import com.cinx.notification.notification.application.query.UserNotificationView;

import java.time.LocalDateTime;

public interface INotificationService {
    /** @param isRead only read (true) or unread (false) ones; null keeps both
     *  @param type only this notification type; null keeps all */
    PageResult<UserNotificationView> getNotifications(String userId, String query, Boolean isRead, String type, int page, int size, String sort);
    Long countUnreadNotifications(String userId);
    NotificationCounts getCounts(String userId);
    void sendNotification(SendNotificationCommand command);
    void sendNotification(SendNotificationCommand command, LocalDateTime sentAt);
    void sendInApp(SendNotificationCommand command);
    void toggleRead(String userId, String notificationId);
    void markAllAsRead(String userId);
    void markAllAsUnread(String userId);
    void deleteNotification(String userId, String notificationId);
}
