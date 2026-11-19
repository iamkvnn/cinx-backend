package com.cinx.notification.notification.infrastructure.websocket;

import com.cinx.notification.notification.application.RealtimeNotificationPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
class StompRealtimeNotificationPublisher implements RealtimeNotificationPublisher {
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void publish(String userId, Map<String, Object> payload) {
        messagingTemplate.convertAndSendToUser(userId, "/queue/notifications", payload);
    }
}
