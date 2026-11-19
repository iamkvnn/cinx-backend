package com.cinx.notification.notification.application;

import java.util.Map;

/** Port: pushes a notification to a user's open sessions as it happens, so the inbox updates without a refresh. */
public interface RealtimeNotificationPublisher {
    void publish(String userId, Map<String, Object> payload);
}
