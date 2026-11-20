package com.cinx.notification.notification.infrastructure.persistence;

import com.cinx.notification.notification.domain.Notification;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationRepositoryAdapterTest {
    @Mock
    private NotificationJpaRepository jpa;

    @Test
    void saveStoresTheContentAndReturnsItWithTheGeneratedId() {
        NotificationRepositoryAdapter adapter = new NotificationRepositoryAdapter(jpa, Mappers.getMapper(NotificationPersistenceMapper.class));
        when(jpa.save(any(NotificationJpaEntity.class))).thenAnswer(call -> {
            NotificationJpaEntity entity = call.getArgument(0);
            entity.setId("notification-1");
            return entity;
        });

        Notification saved = adapter.save(Notification.builder().title("Title").message("Message")
                .metadata(Map.of("courseId", "course-1")).build());

        ArgumentCaptor<NotificationJpaEntity> stored = ArgumentCaptor.forClass(NotificationJpaEntity.class);
        verify(jpa).save(stored.capture());
        assertThat(stored.getValue().getMetadataJson()).isEqualTo("{\"courseId\":\"course-1\"}");
        assertThat(saved.getId()).isEqualTo("notification-1");
        assertThat(saved.getMetadata()).containsEntry("courseId", "course-1");
    }
}
