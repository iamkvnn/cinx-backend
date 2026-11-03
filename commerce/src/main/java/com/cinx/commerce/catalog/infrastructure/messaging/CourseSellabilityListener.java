package com.cinx.commerce.catalog.infrastructure.messaging;

import com.cinx.commerce.catalog.domain.SellableCourse;
import com.cinx.commerce.catalog.infrastructure.messaging.event.CourseSellabilityChanged;
import com.cinx.commerce.catalog.domain.SellableCourseRepository;
import com.cinx.commerce.shared.infrastructure.messaging.InboxMessage;
import com.cinx.commerce.shared.infrastructure.messaging.InboxMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class CourseSellabilityListener {
    private final SellableCourseRepository courseRepository;
    private final InboxMessageRepository inboxRepository;

    @RabbitListener(queues = "${commerce.messaging.course-sellability-queue:commerce.course-sellability}")
    @Transactional
    public void consume(CourseSellabilityChanged event) {
        try {
            inboxRepository.saveAndFlush(InboxMessage.builder()
                    .eventId(event.eventId())
                    .processedAt(LocalDateTime.now())
                    .build());
        } catch (DataIntegrityViolationException duplicate) {
            return;
        }
        courseRepository.save(SellableCourse.builder().courseId(event.courseId()).title(event.title())
                .price(event.price()).categoryId(event.categoryId()).instructorId(event.instructorId())
                .sellable(event.sellable()).updatedAt(event.occurredAt()).build());
    }
}
