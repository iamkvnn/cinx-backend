package com.cinx.reporting.analytics.infrastructure.messaging;

import com.cinx.reporting.analytics.domain.AnalyticsConsentRepository;
import com.cinx.reporting.shared.infrastructure.messaging.InboxMessage;
import com.cinx.reporting.shared.infrastructure.messaging.InboxMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.Map;

/** Keeps the "Thống kê sử dụng" switch from {@code user.learning-profile.updated}; the rest of the profile is not reporting's. */
@Component
@RequiredArgsConstructor
public class AnalyticsConsentListener {
    private final AnalyticsConsentRepository consents;
    private final InboxMessageRepository inbox;
    private final Clock clock;

    @RabbitListener(queues = "reporting.analytics-consent.queue")
    @Transactional
    public void learningProfileUpdated(Map<String, Object> event, @Header(AmqpHeaders.MESSAGE_ID) String eventId) {
        try {
            inbox.saveAndFlush(InboxMessage.builder().messageId(eventId).processedAt(LocalDateTime.now(clock)).build());
        } catch (DataIntegrityViolationException duplicate) {
            return;
        }
        Object userId = event.get("userId");
        if (userId == null) throw new IllegalArgumentException("Missing event field: userId");
        // a producer older than the switch leaves it out, which means on
        boolean enabled = !Boolean.FALSE.equals(event.get("usageAnalyticsConsent"));
        consents.record(String.valueOf(userId), enabled, occurredAt(event.get("occurredAt")));
    }

    private LocalDateTime occurredAt(Object value) {
        try {
            return OffsetDateTime.parse(String.valueOf(value)).withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
        } catch (DateTimeParseException | NullPointerException unreadable) {
            return LocalDateTime.now(clock);
        }
    }
}
