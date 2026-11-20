package com.cinx.notification.dispatch.infrastructure.channel;

import com.cinx.notification.notification.application.INotificationService;
import com.cinx.notification.notification.application.command.SendNotificationCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class InAppNotificationStrategy implements NotificationChannelStrategy {

    private final INotificationService notificationService;

    @Override
    public void send(Map<String, Object> payload) {
        List<String> userIds = null;
        if (payload.containsKey("userIds")) userIds = payload.get("userIds") instanceof List<?> list ? list.stream().map(String::valueOf).toList() : null;

        String title = (String) payload.get("title");
        String message = (String) payload.get("message");
        String type = (String) payload.get("type");
        String referenceId = (String) payload.get("referenceId");
        String actionUrl = (String) payload.get("actionUrl");
        Map<String, Object> metadata = payload.get("metadata") instanceof Map<?, ?> map
                ? map.entrySet().stream().collect(java.util.stream.Collectors.toMap(
                        entry -> String.valueOf(entry.getKey()),
                        Map.Entry::getValue))
                : Map.of();

        if (userIds == null || title == null || message == null) {
            log.error("InApp payload missing required fields: {}", payload);
            throw new IllegalArgumentException("InApp payload missing required fields: 'userId/toUser', 'title', 'message'");
        }

        try {
            notificationService.sendInApp(new SendNotificationCommand(
                    title, message, type, referenceId, actionUrl, metadata, userIds));
            log.info("Saved in-app notification for user {}", userIds);
        } catch (Exception e) {
            log.error("Failed to save in-app notification for user {}", userIds, e);
            throw new RuntimeException("Failed to save in-app notification", e);
        }
    }

    @Override
    public String getChannelName() {
        return "IN_APP";
    }
}
