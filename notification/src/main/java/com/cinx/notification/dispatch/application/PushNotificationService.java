package com.cinx.notification.dispatch.application;

import com.cinx.notification.notification.application.INotificationService;
import com.cinx.notification.device.application.IDeviceTokenService;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PushNotificationService {

    private final INotificationService notificationService;
    private final IDeviceTokenService deviceTokenService;

    public void sendPushNotificationToUser(String userId, String title, String body) {
        sendPushNotificationToUser(userId, title, body, Map.of());
    }

    public void sendPushNotificationToUser(String userId, String title, String body, Map<String, String> data) {
        try {
            List<String> tokens = deviceTokenService.findTokens(userId);
            if (!tokens.isEmpty()) {
                
                Notification notification = Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build();

                MulticastMessage message = MulticastMessage.builder()
                        .addAllTokens(tokens)
                        .setNotification(notification)
                        .putData("click_action", "NOTIFICATION_CLICK") // Adjust based on your frontend
                        .putAllData(data == null ? Map.of() : data)
                        .build();

                FirebaseMessaging.getInstance().sendEachForMulticastAsync(message);
                log.info("Sent push notification to user {}, total tokens: {}", userId, tokens.size());
            } else {
                log.info("User {} has no FCM tokens registered. Skipping push notification.", userId);
            }
        } catch (Exception e) {
            log.error("Failed to send push notification to user {}: {}", userId, e.getMessage());
        }
    }
}
