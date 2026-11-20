package com.cinx.notification.dispatch.application;

import com.cinx.notification.notification.infrastructure.messaging.context.NotificationContext;
import com.cinx.notification.dispatch.infrastructure.channel.NotificationFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationDispatchService implements INotificationDispatchService {

    private final NotificationFactory notificationFactory;

    @Override
    public void dispatch(NotificationContext context) {
        if (context == null || context.channels() == null || context.channels().isEmpty()) {
            log.warn("NotificationDispatchService received empty context, skipping.");
            return;
        }

        Map<String, Map<String, Object>> payloadsByChannel = Map.of(
                "EMAIL",  context.emailPayload()  != null ? context.emailPayload()  : Map.of(),
                "IN_APP", context.inAppPayload()  != null ? context.inAppPayload()  : Map.of(),
                "PUSH",   context.pushPayload()   != null ? context.pushPayload()   : Map.of()
        );

        List<Exception> failures = new ArrayList<>();
        for (String channel : context.channels()) {
            try {
                Map<String, Object> payload = payloadsByChannel.getOrDefault(channel, Map.of());
                if (payload.isEmpty()) {
                    throw new IllegalArgumentException("No payload provided for channel '" + channel + "'");
                }
                notificationFactory.getStrategy(channel).send(payload);
            } catch (Exception e) {
                log.error("Failed to dispatch notification via channel '{}': {}", channel, e.getMessage(), e);
                failures.add(e);
            }
        }
        if (!failures.isEmpty()) {
            RuntimeException exception = new RuntimeException("Failed to dispatch notification through " + failures.size() + " channel(s)");
            failures.forEach(exception::addSuppressed);
            throw exception;
        }
    }
}
