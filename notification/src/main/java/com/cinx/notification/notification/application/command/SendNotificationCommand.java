package com.cinx.notification.notification.application.command;

import java.util.List;
import java.util.Map;

/** Everything needed to notify a set of users: the content once, and who receives it. */
public record SendNotificationCommand(
        String title,
        String message,
        String type,
        String referenceId,
        String actionUrl,
        Map<String, Object> metadata,
        List<String> userIds) {
}
