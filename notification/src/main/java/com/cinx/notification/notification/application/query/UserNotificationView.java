package com.cinx.notification.notification.application.query;

import java.time.LocalDateTime;
import java.util.Map;

/** One row of a user's inbox: a notification's content together with that user's read state. */
public record UserNotificationView(
        String id,
        String userId,
        String title,
        String message,
        String type,
        String referenceId,
        String actionUrl,
        Map<String, Object> metadata,
        boolean read,
        LocalDateTime sentAt) {
}
