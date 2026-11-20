package com.cinx.learning.enrollment.infrastructure.messaging;

import com.cinx.learning.enrollment.infrastructure.messaging.event.CommerceOrderPaid;
import com.cinx.learning.shared.infrastructure.messaging.InboxMessage;
import com.cinx.learning.shared.infrastructure.messaging.InboxMessageRepository;
import com.cinx.learning.enrollment.application.EntitlementService;
import com.cinx.learning.learningpath.application.ILearningPathService;
import com.cinx.learning.progress.application.ILearningProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Component @RequiredArgsConstructor
public class CommerceOrderPaidListener {
    private final InboxMessageRepository inboxRepository;
    private final EntitlementService entitlementService;
    private final ILearningProgressService progressService;
    private final ILearningPathService learningPathService;

    @RabbitListener(queues = "learning.commerce-order-paid.queue", containerFactory = "rabbitListenerContainerFactory")
    @Transactional
    public void consume(CommerceOrderPaid event) {
        try {
            inboxRepository.saveAndFlush(InboxMessage.builder()
                    .eventId(event.eventId())
                    .processedAt(LocalDateTime.now())
                    .build());
        } catch (DataIntegrityViolationException duplicate) {
            return;
        }
        for (CommerceOrderPaid.Item item : event.items()) {
            if (entitlementService.grant(event.orderId(), event.userId(), item.courseId())) {
                progressService.createCourseProgress(event.userId(), item.courseId());
                learningPathService.activatePendingPathForCourse(event.userId(), item.courseId());
            }
        }
    }
}
