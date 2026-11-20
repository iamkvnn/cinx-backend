package com.cinx.reporting.reporting.infrastructure.messaging;

import com.cinx.reporting.reporting.infrastructure.persistence.AccountProjection;
import com.cinx.reporting.reporting.infrastructure.persistence.EnrollmentProjection;
import com.cinx.reporting.reporting.infrastructure.persistence.OrderProjection;
import com.cinx.reporting.reporting.infrastructure.persistence.AccountProjectionRepository;
import com.cinx.reporting.reporting.infrastructure.persistence.EnrollmentProjectionRepository;
import com.cinx.reporting.reporting.infrastructure.persistence.OrderProjectionRepository;
import com.cinx.reporting.shared.infrastructure.messaging.InboxMessage;
import com.cinx.reporting.shared.infrastructure.messaging.InboxMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.math.BigDecimal;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ReportingEventListener {
    private final AccountProjectionRepository accounts;
    private final OrderProjectionRepository orders;
    private final EnrollmentProjectionRepository enrollments;
    private final InboxMessageRepository inbox;

    @RabbitListener(queues = "reporting.auth.queue")
    @Transactional
    public void auth(Map<String, Object> event, @Header(AmqpHeaders.MESSAGE_ID) String eventId) {
        processOnce(eventId, () -> {
            String userId = value(event, "userId");
            AccountProjection account = accounts.findById(userId)
                    .orElseGet(() -> AccountProjection.builder().userId(userId).build());
            if (event.get("changedAt") != null) {
                LocalDateTime changedAt = LocalDateTime.ofInstant(Instant.parse(value(event, "changedAt")), ZoneOffset.UTC);
                if (account.getChangedAt() != null && !changedAt.isAfter(account.getChangedAt())) return;
                account.setChangedAt(changedAt);
                account.setRolesJson(String.valueOf(event.get("roles")));
            }
            if (event.get("status") != null) account.setStatus(value(event, "status"));
            if (event.get("role") != null) account.setRole(value(event, "role"));
            account.setUpdatedAt(LocalDateTime.now());
            accounts.save(account);
        });
    }

    @RabbitListener(queues = "reporting.commerce.queue")
    @Transactional
    public void commerce(Map<String, Object> event, @Header(AmqpHeaders.MESSAGE_ID) String eventId) {
        processOnce(eventId, () -> {
            String orderId = value(event, "orderId");
            OrderProjection order = orders.findById(orderId)
                    .orElseGet(() -> OrderProjection.builder().orderId(orderId).build());
            order.setUserId(value(event, "userId"));
            BigDecimal totalAmount = number(event.get("totalAmount"));
            order.setTotalPrice(event.get("originalAmount") == null ? totalAmount : number(event.get("originalAmount")));
            order.setDiscounted(event.get("discountAmount") == null ? BigDecimal.ZERO : number(event.get("discountAmount")));
            order.setStatus("PAID");
            order.setOrderDate(LocalDateTime.now());
            order.setUpdatedAt(LocalDateTime.now());
            orders.save(order);
        });
    }

    @RabbitListener(queues = "reporting.user.queue")
    @Transactional
    public void user(Map<String, Object> event,
                     @Header(AmqpHeaders.MESSAGE_ID) String eventId,
                     @Header(AmqpHeaders.RECEIVED_ROUTING_KEY) String routingKey) {
        processOnce(eventId, () -> {
            String userId = value(event, "userId");
            AccountProjection account = accounts.findById(userId)
                    .orElseGet(() -> AccountProjection.builder().userId(userId).status("ACTIVE").build());
            account.setRole(switch (routingKey) {
                case "user.instructor.verified" -> "INSTRUCTOR";
                case "user.instructor.pending", "user.instructor.application.submitted" -> "INSTRUCTOR_APPLICANT";
                case "user.instructor.rejected", "user.instructor.terminated" -> "STUDENT";
                default -> account.getRole();
            });
            account.setUpdatedAt(LocalDateTime.now());
            accounts.save(account);
        });
    }

    @RabbitListener(queues = "reporting.learning.queue")
    @Transactional
    public void enrollment(Map<String, Object> event, @Header(AmqpHeaders.MESSAGE_ID) String eventId) {
        processOnce(eventId, () -> {
            String userId = value(event, "userId");
            String courseId = value(event, "courseId");
            enrollments.save(EnrollmentProjection.builder()
                    .id(userId + ":" + courseId)
                    .userId(userId)
                    .courseId(courseId)
                    .grantedAt(date(event.get("grantedAt")))
                    .build());
        });
    }

    private void processOnce(String eventId, Runnable handler) {
        try {
            inbox.saveAndFlush(InboxMessage.builder()
                    .messageId(eventId)
                    .processedAt(LocalDateTime.now())
                    .build());
        } catch (DataIntegrityViolationException duplicate) {
            return;
        }
        handler.run();
    }

    private String value(Map<String, Object> event, String key) {
        Object value = event.get(key);
        if (value == null) throw new IllegalArgumentException("Missing event field: " + key);
        return String.valueOf(value);
    }

    private BigDecimal number(Object value) {
        if (value == null) throw new IllegalArgumentException("Missing monetary event field");
        return value instanceof BigDecimal decimal ? decimal : new BigDecimal(String.valueOf(value));
    }

    private LocalDateTime date(Object value) {
        return value == null ? LocalDateTime.now() : LocalDateTime.parse(String.valueOf(value));
    }
}
