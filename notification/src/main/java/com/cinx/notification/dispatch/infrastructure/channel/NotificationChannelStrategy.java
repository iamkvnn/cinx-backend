package com.cinx.notification.dispatch.infrastructure.channel;

import java.util.Map;

public interface NotificationChannelStrategy {
    
    /**
     * Sends a notification based on event payload.
     * @param payload a map containing context data (like userId, email, pushToken, title, message, body)
     */
    void send(Map<String, Object> payload);
    
    String getChannelName();
}
