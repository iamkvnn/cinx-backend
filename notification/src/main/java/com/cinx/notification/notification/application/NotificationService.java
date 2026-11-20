package com.cinx.notification.notification.application;

import com.cinx.common.exception.NotFoundException;
import com.cinx.common.paging.PageQuery;
import com.cinx.common.paging.PageResult;
import com.cinx.notification.notification.application.command.SendNotificationCommand;
import com.cinx.notification.notification.application.query.NotificationCounts;
import com.cinx.notification.notification.application.query.NotificationQueries;
import com.cinx.notification.notification.application.query.TypeCount;
import com.cinx.notification.notification.application.query.UserNotificationView;
import com.cinx.notification.notification.domain.Notification;
import com.cinx.notification.notification.domain.NotificationRepository;
import com.cinx.notification.notification.domain.UserNotification;
import com.cinx.notification.notification.domain.UserNotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationService implements INotificationService{
    private final UserNotificationRepository userNotificationRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationQueries notificationQueries;
    private final RealtimeNotificationPublisher realtimePublisher;

    @Override
    public PageResult<UserNotificationView> getNotifications(String userId, String query, Boolean isRead, String type, int page, int size, String sort) {
        String resolvedType = type == null || type.isBlank() ? null : type.trim();
        return notificationQueries.findPage(userId, query, isRead, resolvedType, new PageQuery(page, size, sort));
    }

    @Override
    public Long countUnreadNotifications(String userId) {
        return notificationQueries.countUnread(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationCounts getCounts(String userId) {
        Map<String, Long> byType = new LinkedHashMap<>();
        long total = 0;
        long unread = 0;
        for (TypeCount row : notificationQueries.countsByType(userId)) {
            total += row.total();
            unread += row.unread();
            if (row.type() != null) {
                byType.put(row.type(), row.total());
            }
        }
        return new NotificationCounts(total, unread, byType);
    }

    @Override
    public void sendNotification(SendNotificationCommand command) {
        sendNotification(command, LocalDateTime.now());
    }

    @Override
    public void sendNotification(SendNotificationCommand command, LocalDateTime sentAt) {
        Notification notification = notificationRepository.save(Notification.builder()
                    .title(command.title())
                    .message(command.message())
                    .type(command.type())
                    .referenceId(command.referenceId())
                    .actionUrl(command.actionUrl())
                    .metadata(command.metadata())
                    .build());
        userNotificationRepository.saveAll(command.userIds().stream()
                .map(userId -> UserNotification.unread(userId, notification.getId(), sentAt))
                .toList());
    }

    @Transactional
    @Override
    public void sendInApp(SendNotificationCommand command) {
        LocalDateTime sentAt = LocalDateTime.now();
        sendNotification(command, sentAt);
        command.userIds().forEach(userId -> realtimePublisher.publish(userId, Map.of(
                "title", command.title(),
                "message", command.message(),
                "type", command.type() == null ? "" : command.type(),
                "referenceId", command.referenceId() == null ? "" : command.referenceId(),
                "actionUrl", command.actionUrl() == null ? "" : command.actionUrl(),
                "metadata", command.metadata() == null ? Map.of() : command.metadata(),
                "sentAt", sentAt
        )));
    }

    @Override
    public void toggleRead(String userId, String notificationId) {
        UserNotification userNotification = userNotificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new NotFoundException("Notification not found"));
        userNotification.toggleRead();
        userNotificationRepository.save(userNotification);
    }

    @Transactional
    @Override
    public void markAllAsRead(String userId) {
        userNotificationRepository.setReadForAll(userId, true);
    }

    @Transactional
    @Override
    public void markAllAsUnread(String userId) {
        userNotificationRepository.setReadForAll(userId, false);
    }

    @Transactional
    @Override
    public void deleteNotification(String userId, String notificationId) {
        if (!userNotificationRepository.deleteByIdAndUserId(notificationId, userId)) {
            throw new NotFoundException("Notification not found");
        }
    }
}
