package com.cinx.user.profile.infrastructure.messaging;

import com.cinx.user.shared.infrastructure.messaging.OutboxEventPublisher;

import com.cinx.user.profile.domain.User;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class UserEventProducerTest {
    private final OutboxEventPublisher outbox = mock(OutboxEventPublisher.class);
    private final UserEventProducer producer = new UserEventProducer(outbox);

    @Test
    void rejectedInstructorEventCarriesProjectionUserId() {
        producer.sendInstructorRejectedEmail(user());

        assertThat(capturedPayload().get("userId")).isEqualTo("user-1");
    }

    @Test
    void pendingInstructorEventCarriesProjectionUserId() {
        producer.sendNewInstructorNotification(user(), List.of("admin-1"));

        assertThat(capturedPayload().get("userId")).isEqualTo("user-1");
    }

    @Test
    void onboardingEventDoesNotDelegateIdentityRoleChoice() {
        producer.publishProfileOnboarded(user(), "keycloak-subject");

        assertThat(capturedPayload()).containsEntry("userId", "user-1")
                .containsEntry("keycloakSubject", "keycloak-subject")
                .doesNotContainKey("role");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> capturedPayload() {
        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(outbox).enqueue(any(), any(), any(), any(), any(), any(), payload.capture());
        return (Map<String, Object>) payload.getValue();
    }

    private User user() {
        return User.builder()
                .userId("user-1")
                .email("user@example.com")
                .name("User")
                .build();
    }
}
