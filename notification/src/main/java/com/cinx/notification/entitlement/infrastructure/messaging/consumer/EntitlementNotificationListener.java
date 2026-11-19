package com.cinx.notification.entitlement.infrastructure.messaging.consumer;

import com.cinx.notification.entitlement.infrastructure.persistence.EntitlementFeatureProjection;
import com.cinx.notification.entitlement.infrastructure.persistence.EntitlementProjection;
import com.cinx.notification.shared.infrastructure.messaging.InboxMessage;
import com.cinx.notification.entitlement.infrastructure.persistence.EntitlementFeatureProjectionRepository;
import com.cinx.notification.entitlement.infrastructure.persistence.EntitlementProjectionRepository;
import com.cinx.notification.shared.infrastructure.messaging.InboxMessageRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class EntitlementNotificationListener {
    private final ObjectMapper objectMapper;
    private final InboxMessageRepository inboxRepository;
    private final EntitlementProjectionRepository projectionRepository;
    private final EntitlementFeatureProjectionRepository featureRepository;

    @RabbitListener(queues = "notification.subscription-entitlement.queue")
    @Transactional
    public void onSnapshot(JsonNode event, @Header(value = AmqpHeaders.MESSAGE_ID, required = false) String messageId) {
        String eventId = messageId == null || messageId.isBlank() ? event.path("eventId").asText() : messageId;
        if (eventId == null || eventId.isBlank()) eventId = objectMapper.valueToTree(event).toString().hashCode() + "";
        try {
            if (inboxRepository.existsById(eventId)) return;
            JsonNode payload = event.has("payload") ? event.get("payload") : event;
            String userId = payload.path("userId").asText(payload.path("subjectId").asText(null));
            if (userId == null || userId.isBlank()) throw new IllegalArgumentException("Entitlement snapshot userId is required");
            long version = payload.path("entitlementVersion").asLong(payload.path("version").asLong(0));
            EntitlementProjection current = projectionRepository.findById(userId).orElse(null);
            if (current != null && version <= current.getEntitlementVersion()) {
                inboxRepository.save(new InboxMessage(eventId, "STALE", LocalDateTime.now()));
                return;
            }
            if (current == null) current = EntitlementProjection.builder().userId(userId).build();
            current.setEntitlementVersion(version);
            current.setAccessStatus(payload.path("accessStatus").asText("UNKNOWN"));
            current.setUpdatedAt(LocalDateTime.now());
            projectionRepository.save(current);
            featureRepository.deleteAllByUserId(userId);
            JsonNode features = payload.path("features");
            if (features.isArray()) {
                for (JsonNode feature : features) {
                    featureRepository.save(EntitlementFeatureProjection.builder()
                            .userId(userId)
                            .featureKey(feature.path("featureKey").asText(feature.path("key").asText()))
                            .enabled(feature.path("enabled").asBoolean(true))
                            .quotaLimit(feature.hasNonNull("quotaLimit") ? feature.get("quotaLimit").asLong()
                                    : (feature.hasNonNull("limit") ? feature.get("limit").asLong() : null))
                            .entitlementVersion(version)
                            .build());
                }
            }
            inboxRepository.save(new InboxMessage(eventId, "SUCCESS", LocalDateTime.now()));
        } catch (DataIntegrityViolationException duplicate) {
            log.debug("Duplicate entitlement notification event {}", eventId);
        }
    }
}
