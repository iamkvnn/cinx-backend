package com.cinx.auth.identity.infrastructure.messaging;

import com.cinx.auth.identity.application.IIdentityService;
import com.cinx.auth.identity.infrastructure.persistence.AuthEventInbox;
import com.cinx.auth.identity.infrastructure.persistence.AuthEventInboxRepository;

import com.cinx.auth.identity.application.command.LinkIdentityCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserLifecycleListener {
    private final IIdentityService identityService;
    private final AuthEventInboxRepository inbox;
    private final Clock clock;

    @RabbitListener(queues = "auth.user-lifecycle.queue")
    @Transactional
    void handle(Map<String, Object> event,
                @Header(AmqpHeaders.RECEIVED_ROUTING_KEY) String routingKey,
                @Header(AmqpHeaders.MESSAGE_ID) String eventId) {
        if (inbox.existsById(eventId)) return;
        inbox.saveAndFlush(AuthEventInbox.builder().eventId(eventId)
                .processedAt(LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC)).build());
        String userId = required(event, "userId");
        switch (routingKey) {
            case "user.profile.onboarded" -> identityService.link(
                    userId,
                    new LinkIdentityCommand(
                            required(event, "keycloakSubject"),
                            nullable(event, "email")));
            case "user.instructor.verified" -> identityService.promoteInstructor(userId);
            case "user.instructor.terminated" -> identityService.terminateInstructor(userId);
            default -> log.warn("Ignoring unsupported user lifecycle event routingKey={}", routingKey);
        }
    }

    private String required(Map<String, Object> event, String key) {
        String value = nullable(event, key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing user lifecycle event field: " + key);
        }
        return value;
    }

    private String nullable(Map<String, Object> event, String key) {
        Object value = event.get(key);
        return value == null ? null : String.valueOf(value);
    }
}
