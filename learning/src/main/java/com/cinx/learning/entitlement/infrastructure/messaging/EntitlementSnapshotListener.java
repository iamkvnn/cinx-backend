package com.cinx.learning.entitlement.infrastructure.messaging;

import com.cinx.learning.entitlement.infrastructure.persistence.EntitlementEventInbox;
import com.cinx.learning.entitlement.infrastructure.persistence.UserEntitlementProjection;
import com.cinx.learning.entitlement.infrastructure.persistence.UserFeatureEntitlement;
import com.cinx.learning.entitlement.infrastructure.persistence.EntitlementEventInboxRepository;
import com.cinx.learning.entitlement.infrastructure.persistence.UserEntitlementProjectionRepository;
import com.cinx.learning.entitlement.infrastructure.persistence.UserFeatureEntitlementRepository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.cinx.learning.entitlement.infrastructure.messaging.event.CommerceSubscriptionEntitlementsChanged;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Component
@RequiredArgsConstructor
public class EntitlementSnapshotListener {
    private final EntitlementEventInboxRepository inbox;
    private final UserEntitlementProjectionRepository projections;
    private final UserFeatureEntitlementRepository features;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @RabbitListener(queues = "learning.subscription-entitlement.queue", containerFactory = "rabbitListenerContainerFactory")
    @Transactional
    public void consume(CommerceSubscriptionEntitlementsChanged event) {
        String eventId = event.eventId() == null || event.eventId().isBlank()
                ? event.subjectId() + ":" + event.entitlementVersion() : event.eventId();
        try {
            inbox.saveAndFlush(EntitlementEventInbox.builder().eventId(eventId).userId(event.userId())
                    .entitlementVersion(event.entitlementVersion()).receivedAt(now()).build());
        } catch (DataIntegrityViolationException duplicate) {
            return;
        }
        UserEntitlementProjection current = projections.lockByUserId(event.userId()).orElse(null);
        if (current != null && event.entitlementVersion() <= current.getEntitlementVersion()) {
            return;
        }
        String snapshotHash = hash(event);
        if (current == null) {
            current = UserEntitlementProjection.builder().userId(event.userId()).build();
        }
        current.setSubjectId(event.subjectId());
        current.setSourceSummaryJson(json(event.sources()));
        current.setEntitlementVersion(event.entitlementVersion());
        current.setAccessStatus(event.accessStatus());
        current.setEffectiveFrom(toLocal(event.effectiveFrom()));
        current.setEffectiveUntil(toLocal(event.accessEffectiveUntil()));
        current.setReconcileBy(toLocal(event.reconcileBy()));
        current.setSnapshotHash(snapshotHash);
        current.setUpdatedAt(now());
        projections.save(current);
        features.deleteAllByUserId(event.userId());
        for (CommerceSubscriptionEntitlementsChanged.Feature feature : event.features() == null ? java.util.List.<CommerceSubscriptionEntitlementsChanged.Feature>of() : event.features()) {
            features.save(UserFeatureEntitlement.builder().userId(event.userId()).featureKey(feature.key())
                    .valueType(feature.type()).enabled(feature.enabled()).quotaLimit(feature.limit())
                    .quotaUnlimited(feature.unlimited()).quotaUnit(feature.unit()).resetPolicy(feature.resetPolicy())
                    .quotaPeriodStart(toLocal(feature.periodStart())).quotaPeriodEnd(toLocal(feature.periodEnd()))
                    .effectiveUntil(toLocal(feature.effectiveUntil())).updatedAt(now()).build());
        }
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize entitlement source summary", exception);
        }
    }

    private String hash(Object value) {
        return hex(digest(json(value).getBytes(StandardCharsets.UTF_8)));
    }

    private byte[] digest(byte[] value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to hash entitlement snapshot", exception);
        }
    }

    private String hex(byte[] bytes) {
        StringBuilder result = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) result.append(String.format("%02x", value));
        return result.toString();
    }

    private LocalDateTime now() {
        return LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
    }

    private LocalDateTime toLocal(java.time.Instant value) {
        return value == null ? null : LocalDateTime.ofInstant(value, ZoneOffset.UTC);
    }
}
