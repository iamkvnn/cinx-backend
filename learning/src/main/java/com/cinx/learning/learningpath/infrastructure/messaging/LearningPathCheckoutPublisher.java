package com.cinx.learning.learningpath.infrastructure.messaging;

import com.cinx.learning.learningpath.application.LearningPathCheckouts;
import com.cinx.learning.shared.infrastructure.messaging.OutboxEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class LearningPathCheckoutPublisher implements LearningPathCheckouts {
    private final OutboxEventPublisher outbox;

    @Override
    public void requestCheckout(String userId, String courseId) {
        String eventId = UUID.randomUUID().toString();
        outbox.enqueue(eventId, "LearningPath", courseId,
                "LearningPathCheckoutRequested", "cinx.domain.events",
                "learning.path.checkout.requested", Map.of(
                        "eventId", eventId,
                        "userId", userId,
                        "courseId", courseId));
    }
}
