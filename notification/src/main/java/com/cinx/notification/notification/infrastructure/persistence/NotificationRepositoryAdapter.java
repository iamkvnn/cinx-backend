package com.cinx.notification.notification.infrastructure.persistence;

import com.cinx.notification.notification.domain.Notification;
import com.cinx.notification.notification.domain.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class NotificationRepositoryAdapter implements NotificationRepository {
    private final NotificationJpaRepository jpa;
    private final NotificationPersistenceMapper mapper;

    @Override
    public Notification save(Notification notification) {
        return mapper.toDomain(jpa.save(mapper.toEntity(notification)));
    }
}
