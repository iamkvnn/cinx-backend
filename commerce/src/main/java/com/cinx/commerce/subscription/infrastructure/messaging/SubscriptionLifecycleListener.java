package com.cinx.commerce.subscription.infrastructure.messaging;

import com.cinx.commerce.subscription.application.SubscriptionProvisioningService;
import com.cinx.commerce.subscription.infrastructure.persistence.AuthEligibilityEventInbox;
import com.cinx.commerce.subscription.infrastructure.persistence.AuthEligibilityProjection;
import com.cinx.commerce.subscription.infrastructure.persistence.AuthEligibilityEventInboxRepository;
import com.cinx.commerce.subscription.infrastructure.persistence.AuthEligibilityProjectionRepository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class SubscriptionLifecycleListener {
    private final AuthEligibilityEventInboxRepository inbox;
    private final AuthEligibilityProjectionRepository projections;
    private final SubscriptionProvisioningService provisioning;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @RabbitListener(queues = "commerce.auth-authorization.queue")
    @Transactional
    public void onAuthorizationChanged(Map<String, Object> event,
                                       @Header(value = AmqpHeaders.RECEIVED_ROUTING_KEY, required = false) String routingKey,
                                       @Header(value = AmqpHeaders.MESSAGE_ID, required = false) String messageId) {
        String userId = string(event, "userId");
        if (userId == null) throw new IllegalArgumentException("AuthAuthorizationChanged requires userId");
        LocalDateTime changedAt = instant(string(event, "changedAt"));
        String eventId = messageId == null || messageId.isBlank()
                ? userId + ":" + changedAt : messageId;
        try {
            inbox.saveAndFlush(AuthEligibilityEventInbox.builder().eventId(eventId).userId(userId)
                    .receivedAt(now()).build());
        } catch (DataIntegrityViolationException duplicate) {
            return;
        }
        AuthEligibilityProjection current = projections.findById(userId).orElse(null);
        if (current != null && !changedAt.isAfter(current.getChangedAt())) return;
        List<String> roles = strings(event.get("roles"));
        if (current == null) current = AuthEligibilityProjection.builder().userId(userId).build();
        current.setAccountStatus(string(event, "status", "UNKNOWN"));
        current.setRolesJson(json(roles));
        current.setChangedAt(changedAt);
        current.setUpdatedAt(now());
        projections.save(current);
        provisioning.applyAuthorization(userId, current.getAccountStatus(), roles);
    }

    @RabbitListener(queues = "commerce.user-onboarding.queue")
    @Transactional
    public void onProfileOnboarded(Map<String, Object> event,
                                   @Header(value = AmqpHeaders.MESSAGE_ID, required = false) String messageId) {
        String userId = string(event, "userId");
        if (userId == null) throw new IllegalArgumentException("UserProfileOnboarded requires userId");
        String eventId = messageId == null || messageId.isBlank() ? "onboard:" + userId : messageId;
        try {
            inbox.saveAndFlush(AuthEligibilityEventInbox.builder().eventId(eventId).userId(userId)
                    .receivedAt(now()).build());
        } catch (DataIntegrityViolationException duplicate) {
            return;
        }
        provisioning.ensureFreeSubscription(userId, "USER_PROFILE_ONBOARDED");
    }

    private String string(Map<String, Object> event, String key) {
        return string(event, key, null);
    }

    private String string(Map<String, Object> event, String key, String fallback) {
        Object value = event.get(key);
        if (value == null) return fallback;
        String result = String.valueOf(value);
        return result.isBlank() ? fallback : result;
    }

    private LocalDateTime instant(String value) {
        if (value == null) throw new IllegalArgumentException("AuthAuthorizationChanged requires changedAt");
        return LocalDateTime.ofInstant(Instant.parse(value), ZoneOffset.UTC);
    }

    private List<String> strings(Object value) {
        if (!(value instanceof List<?> values)) return List.of();
        return values.stream().map(String::valueOf).filter(v -> !v.isBlank()).sorted().toList();
    }

    private String json(Object value) {
        try { return objectMapper.writeValueAsString(value); }
        catch (JsonProcessingException exception) { throw new IllegalStateException("Unable to persist auth projection", exception); }
    }

    private LocalDateTime now() {
        return LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
    }
}
