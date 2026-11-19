package com.cinx.notification.device.infrastructure.persistence;

import com.cinx.notification.device.domain.UserDeviceToken;
import com.cinx.notification.device.domain.UserDeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class UserDeviceTokenRepositoryAdapter implements UserDeviceTokenRepository {
    private final UserDeviceTokenJpaRepository jpa;
    private final UserDeviceTokenPersistenceMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public Optional<UserDeviceToken> findByFcmToken(String fcmToken) {
        return jpa.findByFcmToken(fcmToken).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDeviceToken> findByUserId(String userId) {
        return jpa.findByUserId(userId).stream().map(mapper::toDomain).toList();
    }

    /** Inserts a new token, or writes back the two fields that change when a device changes hands. */
    @Override
    @Transactional
    public UserDeviceToken save(UserDeviceToken token) {
        if (token.getId() == null) {
            return mapper.toDomain(jpa.save(mapper.toEntity(token)));
        }
        UserDeviceTokenJpaEntity entity = jpa.findById(token.getId())
                .orElseThrow(() -> new IllegalStateException("Device token " + token.getId() + " no longer exists"));
        entity.setUserId(token.getUserId());
        entity.setDeviceInfo(token.getDeviceInfo());
        return mapper.toDomain(jpa.save(entity));
    }
}
