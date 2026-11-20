package com.cinx.learning.learningpath.infrastructure.messaging;

import com.cinx.learning.shared.infrastructure.messaging.OutboxEventPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LearningPathCheckoutPublisherTest {
    @Mock
    private OutboxEventPublisher outbox;

    @InjectMocks
    private LearningPathCheckoutPublisher publisher;

    @Test
    @SuppressWarnings("unchecked")
    void aCheckoutRequestTravelsTheDomainExchangeWithItsEventIdUserAndCourse() {
        ArgumentCaptor<String> eventId = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);

        publisher.requestCheckout("user-1", "course-1");

        verify(outbox).enqueue(eventId.capture(), eq("LearningPath"), eq("course-1"), eq("LearningPathCheckoutRequested"),
                eq("cinx.domain.events"), eq("learning.path.checkout.requested"), payload.capture());
        Map<String, Object> body = (Map<String, Object>) payload.getValue();
        assertThat(body).containsEntry("userId", "user-1").containsEntry("courseId", "course-1");
        // the consumer deduplicates on the event id inside the payload: it must be the one the outbox row carries
        assertThat(body.get("eventId")).isEqualTo(eventId.getValue());
    }
}
