package com.cinx.user.policy.infrastructure.messaging;

import com.cinx.user.policy.application.PolicyEvents;
import com.cinx.user.shared.infrastructure.messaging.OutboxEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class PolicyEventPublisher implements PolicyEvents {
    private static final String EXCHANGE = "user.events.exchange";

    private final OutboxEventPublisher outboxEventPublisher;

    @Override
    public void publishPolicyPublished(
            String documentId,
            String title,
            String sourceUrl,
            String content,
            Integer versionNumber,
            LocalDateTime publishedAt
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("documentId", documentId);
        payload.put("title", title);
        payload.put("sourceType", "POLICY");
        payload.put("sourceUrl", sourceUrl);
        payload.put("content", content);
        payload.put("versionNumber", versionNumber);
        payload.put("publishedAt", publishedAt);
        publish("user.policy.published", payload);
    }

    @Override
    public void publishPolicyArchived(String documentId, String sourceUrl, LocalDateTime publishedAt) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("documentId", documentId);
        payload.put("sourceType", "POLICY");
        payload.put("sourceUrl", sourceUrl);
        payload.put("publishedAt", publishedAt);
        publish("user.policy.archived", payload);
    }

    private void publish(String routingKey, Map<String, Object> payload) {
        outboxEventPublisher.enqueue(
                UUID.randomUUID().toString(),
                "UserEvent",
                routingKey,
                routingKey,
                EXCHANGE,
                routingKey,
                payload
        );
        log.info("User event published -> routingKey={}", routingKey);
    }
}
