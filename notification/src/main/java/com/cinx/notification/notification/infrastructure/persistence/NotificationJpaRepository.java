package com.cinx.notification.notification.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface NotificationJpaRepository extends JpaRepository<NotificationJpaEntity, String> {
}
