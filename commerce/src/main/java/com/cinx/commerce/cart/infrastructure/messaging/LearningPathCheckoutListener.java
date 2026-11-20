package com.cinx.commerce.cart.infrastructure.messaging;

import com.cinx.commerce.cart.domain.CartItem;
import com.cinx.commerce.cart.domain.CartRepository;
import com.cinx.commerce.cart.infrastructure.messaging.event.LearningPathCheckoutRequested;
import com.cinx.commerce.shared.infrastructure.messaging.InboxMessage;
import com.cinx.commerce.shared.infrastructure.messaging.InboxMessageRepository;
import com.cinx.commerce.catalog.application.SellableCourses;
import com.cinx.common.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.UUID;

@Component @RequiredArgsConstructor
public class LearningPathCheckoutListener {
    private final InboxMessageRepository inboxRepository;
    private final CartRepository cartRepository;
    private final SellableCourses courses;

    @RabbitListener(queues = "commerce.learning-path-checkout")
    @Transactional
    public void consume(LearningPathCheckoutRequested event) {
        try {
            inboxRepository.saveAndFlush(InboxMessage.builder()
                    .eventId(event.eventId())
                    .processedAt(LocalDateTime.now())
                    .build());
        } catch (DataIntegrityViolationException duplicate) {
            return;
        }
        courses.findSellable(event.courseId())
                .orElseThrow(() -> new BadRequestException("Course is not sellable"));
        if (!cartRepository.existsByUserIdAndCourseId(event.userId(), event.courseId())) {
            cartRepository.save(CartItem.builder().id(UUID.randomUUID().toString()).userId(event.userId())
                    .courseId(event.courseId()).addedAt(LocalDateTime.now()).build());
        }
    }
}
