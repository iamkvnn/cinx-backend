package com.cinx.reporting.reporting.infrastructure.messaging;

import com.cinx.reporting.reporting.infrastructure.persistence.EntitlementFeatureProjection;
import com.cinx.reporting.reporting.infrastructure.persistence.EntitlementProjection;
import com.cinx.reporting.shared.infrastructure.messaging.InboxMessage;
import com.cinx.reporting.reporting.infrastructure.persistence.EntitlementFeatureProjectionRepository;
import com.cinx.reporting.reporting.infrastructure.persistence.EntitlementProjectionRepository;
import com.cinx.reporting.shared.infrastructure.messaging.InboxMessageRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class EntitlementProjectionListener {
    private final InboxMessageRepository inboxRepository;
    private final EntitlementProjectionRepository projectionRepository;
    private final EntitlementFeatureProjectionRepository featureRepository;

    @RabbitListener(queues = "reporting.subscription-entitlement.queue")
    @Transactional
    public void onSnapshot(JsonNode event, @Header(value = AmqpHeaders.MESSAGE_ID, required = false) String messageId) {
        String eventId = messageId;
        if (eventId == null || eventId.isBlank()) eventId = event.path("eventId").asText("unknown");
        try {
            if (inboxRepository.existsById(eventId)) return;
            JsonNode payload = event.has("payload") ? event.get("payload") : event;
            String userId = payload.path("userId").asText(payload.path("subjectId").asText(null));
            if (userId == null || userId.isBlank()) throw new IllegalArgumentException("Entitlement snapshot userId is required");
            long version = payload.path("entitlementVersion").asLong(payload.path("version").asLong(0));
            EntitlementProjection current = projectionRepository.findById(userId).orElse(null);
            if (current != null && version <= current.getEntitlementVersion()) {
                inboxRepository.save(InboxMessage.builder().messageId(eventId).processedAt(LocalDateTime.now()).build());
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
            inboxRepository.save(InboxMessage.builder().messageId(eventId).processedAt(LocalDateTime.now()).build());
        } catch (DataIntegrityViolationException duplicate) {
            // Redelivery is a successful no-op; the broker can acknowledge it.
        }
    }
}
