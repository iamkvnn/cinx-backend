package com.cinx.notification.notification.infrastructure.persistence;

import com.cinx.notification.notification.domain.UserNotification;
import com.cinx.notification.notification.domain.UserNotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class UserNotificationRepositoryAdapter implements UserNotificationRepository {
    private final UserNotificationJpaRepository jpa;
    private final UserNotificationPersistenceMapper mapper;

    @Override
    @Transactional
    public List<UserNotification> saveAll(List<UserNotification> userNotifications) {
        return jpa.saveAll(userNotifications.stream().map(mapper::toEntity).toList()).stream().map(mapper::toDomain).toList();
    }

    /** Inserts a new row, or writes back the one thing that can change after sending: the read flag. */
    @Override
    @Transactional
    public UserNotification save(UserNotification userNotification) {
        if (userNotification.getId() == null) {
            return mapper.toDomain(jpa.save(mapper.toEntity(userNotification)));
        }
        UserNotificationJpaEntity entity = jpa.findById(userNotification.getId())
                .orElseThrow(() -> new IllegalStateException("User notification " + userNotification.getId() + " no longer exists"));
        entity.setIsRead(userNotification.isRead());
        return mapper.toDomain(jpa.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserNotification> findByIdAndUserId(String id, String userId) {
        return jpa.findByIdAndUserId(id, userId).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public boolean deleteByIdAndUserId(String id, String userId) {
        return jpa.deleteByIdAndUserId(id, userId) > 0;
    }

    @Override
    @Transactional
    public void setReadForAll(String userId, boolean read) {
        jpa.updateReadStatusByUserId(userId, read);
    }
}
