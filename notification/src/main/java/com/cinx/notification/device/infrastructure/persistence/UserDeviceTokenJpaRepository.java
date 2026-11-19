package com.cinx.notification.device.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface UserDeviceTokenJpaRepository extends JpaRepository<UserDeviceTokenJpaEntity, String> {
    Optional<UserDeviceTokenJpaEntity> findByFcmToken(String fcmToken);

    List<UserDeviceTokenJpaEntity> findByUserId(String userId);
}
