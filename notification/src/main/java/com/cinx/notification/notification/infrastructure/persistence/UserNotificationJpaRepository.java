package com.cinx.notification.notification.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

interface UserNotificationJpaRepository extends JpaRepository<UserNotificationJpaEntity, String> {
    /** The caller's notifications; every filter is optional and they combine. */
    @Query("SELECT un FROM UserNotificationJpaEntity un JOIN un.notification n WHERE un.userId = :userId AND " +
            "(:query IS NULL OR n.title LIKE %:query% OR n.message LIKE %:query%) AND " +
            "(:isRead IS NULL OR un.isRead = :isRead) AND " +
            "(:type IS NULL OR n.type = :type)")
    Page<UserNotificationJpaEntity> findByUserId(String query, String userId, Boolean isRead, String type, Pageable pageable);

    /** Rows of {type, notifications of that type, of which unread} for the caller. */
    @Query("SELECT n.type, COUNT(un), SUM(CASE WHEN un.isRead = false THEN 1L ELSE 0L END) " +
            "FROM UserNotificationJpaEntity un JOIN un.notification n WHERE un.userId = :userId GROUP BY n.type")
    List<Object[]> countByTypeForUser(String userId);

    @Query("SELECT COUNT(un) FROM UserNotificationJpaEntity un WHERE un.userId = :userId AND un.isRead = false")
    Long countByUserIdAndReadFalse(String userId);

    Optional<UserNotificationJpaEntity> findByIdAndUserId(String id, String userId);

    @Modifying
    @Query("DELETE FROM UserNotificationJpaEntity un WHERE un.id = :id AND un.userId = :userId")
    int deleteByIdAndUserId(String id, String userId);

    @Modifying
    @Query("UPDATE UserNotificationJpaEntity un SET un.isRead = :isRead WHERE un.userId = :userId")
    void updateReadStatusByUserId(String userId, Boolean isRead);
}
