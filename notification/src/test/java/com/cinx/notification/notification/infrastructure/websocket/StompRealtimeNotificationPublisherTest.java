package com.cinx.notification.notification.infrastructure.websocket;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Map;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StompRealtimeNotificationPublisherTest {
    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Test
    void publishSendsToThatUsersNotificationQueue() {
        Map<String, Object> payload = Map.of("title", "Title");

        new StompRealtimeNotificationPublisher(messagingTemplate).publish("user-1", payload);

        // the client subscribes to /user/queue/notifications; Spring resolves the user prefix from the destination user
        verify(messagingTemplate).convertAndSendToUser("user-1", "/queue/notifications", payload);
    }
}
